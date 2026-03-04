package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fragment for creating a new event.
 */
public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etDescription;
    private Button btnConfirm, btnCancel, btnStartDate, btnEndDate;
    private Date startDate, endDate;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        etName = view.findViewById(R.id.et_event_name);
        etLocation = view.findViewById(R.id.et_event_location);
        etDescription = view.findViewById(R.id.et_event_description);
        btnConfirm = view.findViewById(R.id.btn_confirm);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnStartDate = view.findViewById(R.id.btn_start_date);
        btnEndDate = view.findViewById(R.id.btn_end_date);

        // Setup date-time pickers
        btnStartDate.setOnClickListener(v -> showDateTimePicker(true));
        btnEndDate.setOnClickListener(v -> showDateTimePicker(false));

        // Setup cancel button to show custom confirmation dialog
        btnCancel.setOnClickListener(v -> showCancelConfirmationDialog());

        // Setup confirm button to save the event
        btnConfirm.setOnClickListener(v -> handleCreateEvent());
    }

    /**
     * Shows a Material Date Picker followed by a Time Picker and updates the selected date-time.
     * @param isStartDate True if picking the start date-time, false for end date-time.
     */
    private void showDateTimePicker(boolean isStartDate) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStartDate ? "Select Start Date" : "Select End Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);
            
            // Sync the local calendar with the date selected in UTC
            calendar.set(utcCalendar.get(Calendar.YEAR), 
                         utcCalendar.get(Calendar.MONTH), 
                         utcCalendar.get(Calendar.DAY_OF_MONTH));

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText(isStartDate ? "Select Start Time" : "Select End Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                Date selectedDateTime = calendar.getTime();
                if (isStartDate) {
                    startDate = selectedDateTime;
                    btnStartDate.setText(dateTimeFormat.format(startDate));
                } else {
                    endDate = selectedDateTime;
                    btnEndDate.setText(dateTimeFormat.format(endDate));
                }
            });

            timePicker.show(getParentFragmentManager(), "TIME_PICKER");
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    /**
     * Records fields, creates an Event object, and saves it to the database.
     */
    private void handleCreateEvent() {
        String name = etName.getText().toString().trim();
        String locationStr = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        if (startDate == null || endDate == null) {
            Toast.makeText(requireContext(), "Please select both start and end dates/times", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.before(startDate)) {
            Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // Fetch current user to use as Organizer
        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            Organizer organizer = new Organizer(user.getDeviceId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());

            // Using selected date-times. For simplicity, setting registration dates same as event dates.
            Event.create(name, description, organizer, null, null, startDate, endDate, startDate, endDate)
                    .addOnSuccessListener(event -> {
                        event.save().addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                            try {
                                getParentFragmentManager().popBackStack();
                            } catch (IllegalStateException e) {
                                Log.e("CreateEventFragment", "Error while popping back stack", e);
                            }
                        }).addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to create event object", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    /**
     * Displays a custom confirmation dialog asking the user if they are sure they want to cancel.
     */
    private void showCancelConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancel_confirmation, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(dialogView)
                .create();

        ImageView closeIcon = dialogView.findViewById(R.id.iv_close);
        Button backButton = dialogView.findViewById(R.id.btn_dialog_back);
        Button yesButton = dialogView.findViewById(R.id.btn_dialog_yes);

        closeIcon.setOnClickListener(v -> dialog.dismiss());
        backButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                getParentFragmentManager().popBackStack();
            } catch (IllegalStateException e) {
                Log.e("CreateEventFragment", "Error while popping back stack", e);
            }
        });

        dialog.show();
    }
}
