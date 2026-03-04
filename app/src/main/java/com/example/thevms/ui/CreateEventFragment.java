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
    private Button btnConfirm, btnCancel;
    private Button btnRegStartDate, btnRegEndDate, btnEventStartDate, btnEventEndDate;
    private Date regStartDate, regEndDate, eventStartDate, eventEndDate;
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
        
        btnRegStartDate = view.findViewById(R.id.btn_reg_start_date);
        btnRegEndDate = view.findViewById(R.id.btn_reg_end_date);
        btnEventStartDate = view.findViewById(R.id.btn_event_start_date);
        btnEventEndDate = view.findViewById(R.id.btn_event_end_date);

        // Setup date-time pickers
        btnRegStartDate.setOnClickListener(v -> showDateTimePicker(1));
        btnRegEndDate.setOnClickListener(v -> showDateTimePicker(2));
        btnEventStartDate.setOnClickListener(v -> showDateTimePicker(3));
        btnEventEndDate.setOnClickListener(v -> showDateTimePicker(4));

        // Setup cancel button to show custom confirmation dialog
        btnCancel.setOnClickListener(v -> showCancelConfirmationDialog());

        // Setup confirm button to save the event
        btnConfirm.setOnClickListener(v -> handleCreateEvent());
    }

    /**
     * Shows a Material Date Picker followed by a Time Picker and updates the selected date-time.
     * @param type 1: RegStart, 2: RegEnd, 3: EventStart, 4: EventEnd
     */
    private void showDateTimePicker(int type) {
        String title = "";
        switch (type) {
            case 1: title = "Select Registration Start Date"; break;
            case 2: title = "Select Registration End Date"; break;
            case 3: title = "Select Event Start Date"; break;
            case 4: title = "Select Event End Date"; break;
        }

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);
            
            calendar.set(utcCalendar.get(Calendar.YEAR), 
                         utcCalendar.get(Calendar.MONTH), 
                         utcCalendar.get(Calendar.DAY_OF_MONTH));

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Select Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                Date selectedDateTime = calendar.getTime();
                switch (type) {
                    case 1:
                        regStartDate = selectedDateTime;
                        btnRegStartDate.setText(dateTimeFormat.format(regStartDate));
                        break;
                    case 2:
                        regEndDate = selectedDateTime;
                        btnRegEndDate.setText(dateTimeFormat.format(regEndDate));
                        break;
                    case 3:
                        eventStartDate = selectedDateTime;
                        btnEventStartDate.setText(dateTimeFormat.format(eventStartDate));
                        break;
                    case 4:
                        eventEndDate = selectedDateTime;
                        btnEventEndDate.setText(dateTimeFormat.format(eventEndDate));
                        break;
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

        if (locationStr.isEmpty()) {
            etLocation.setError("Location is required");
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            return;
        }

        if (regStartDate == null || regEndDate == null || eventStartDate == null || eventEndDate == null) {
            Toast.makeText(requireContext(), "Please select all dates and times", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chronological validation: regStart < regEnd < eventStart < eventEnd
        if (!regEndDate.after(regStartDate)) {
            Toast.makeText(requireContext(), "Registration end must be after registration start", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!eventStartDate.after(regEndDate)) {
            Toast.makeText(requireContext(), "Event start must be after registration end", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!eventEndDate.after(eventStartDate)) {
            Toast.makeText(requireContext(), "Event end must be after event start", Toast.LENGTH_SHORT).show();
            return;
        }

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            Organizer organizer = new Organizer(user.getDeviceId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());

            // Note: Currently locationStr is a String, but Event model expects android.location.Location.
            // For now, passing null for Location as in previous implementation, but validating the text field.
            Event.create(name, description, organizer, null, null, regStartDate, regEndDate, eventStartDate, eventEndDate)
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
