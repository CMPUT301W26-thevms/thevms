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
import androidx.annotation.VisibleForTesting;
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
    private com.google.android.material.materialswitch.MaterialSwitch switchGeo;
    private View layoutLimitDistance;
    private EditText etLimitDistance;

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

        switchGeo = view.findViewById(R.id.switch_geolocation);
        layoutLimitDistance = view.findViewById(R.id.layout_limit_distance);
        etLimitDistance = view.findViewById(R.id.et_limit_distance);
        layoutLimitDistance.setVisibility(View.GONE);
        // If switch is on, show limit distance field, else hide limit distance field
        switchGeo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutLimitDistance.setVisibility(View.VISIBLE);
            } else {
                layoutLimitDistance.setVisibility(View.GONE);
            }
        });

        // Setup cancel button to show custom confirmation dialog
        btnCancel.setOnClickListener(v -> showCancelConfirmationDialog());

        // Setup confirm button to save the event
        btnConfirm.setOnClickListener(v -> handleCreateEvent());
    }

    /**
     * Testing helper to manually set dates and bypass picker UI.
     */
    @VisibleForTesting
    public void setTestingDates(Date rs, Date re, Date es, Date ee) {
        this.regStartDate = rs;
        this.regEndDate = re;
        this.eventStartDate = es;
        this.eventEndDate = ee;
        
        if (btnRegStartDate != null) btnRegStartDate.setText(dateTimeFormat.format(rs));
        if (btnRegEndDate != null) btnRegEndDate.setText(dateTimeFormat.format(re));
        if (btnEventStartDate != null) btnEventStartDate.setText(dateTimeFormat.format(es));
        if (btnEventEndDate != null) btnEventEndDate.setText(dateTimeFormat.format(ee));
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
        boolean isGeoRequired = switchGeo.isChecked();

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

        // If geolocation is required, validate radius, radius must be greater than 0
        double radius;
        if (isGeoRequired) {
            try {
                radius = Double.parseDouble(etLimitDistance.getText().toString().trim());
                if (radius <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid number for distance", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number for distance", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            radius = 0.0;
        }

        android.location.Location eventLocation = getLocationFromAddress(locationStr);
        if (eventLocation == null) {
            Toast.makeText(requireContext(), "Please enter a valid location", Toast.LENGTH_SHORT).show();
            return;
        }

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            Organizer organizer = new Organizer(user.getDeviceId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());

            // Note: Currently locationStr is a String, but Event model expects android.location.Location.
            // Updated: location is now the coordinates of the event, not the location string.
            Event.create(name, description, organizer, locationStr, null, regStartDate, regEndDate, eventStartDate, eventEndDate, isGeoRequired, radius, eventLocation)
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

    private android.location.Location getLocationFromAddress(String strAddress) {
        android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(), Locale.getDefault());
        try {
            java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(strAddress, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                android.location.Location location = new android.location.Location("event_location");
                location.setLatitude(address.getLatitude());
                location.setLongitude(address.getLongitude());
                return location;
            }
        } catch (Exception e) {
            Log.e("CreateEventFragment", "Error while getting location", e);
        }
        return null;
    }
}
