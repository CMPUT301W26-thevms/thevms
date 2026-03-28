package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fragment for creating a new event.
 * Handles event details input, date-time selection, and geolocation settings.
 */
public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etDescription;
    private EditText etMaxAttendees, etMaxWaitlist;
    private Button btnConfirm, btnCancel, btnPreviewLocation;
    private Button btnRegStartDate, btnRegEndDate, btnEventStartDate, btnEventEndDate;
    private Date regStartDate, regEndDate, eventStartDate, eventEndDate;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private com.google.android.material.materialswitch.MaterialSwitch switchGeo;
    private View layoutLimitDistance;
    private EditText etLimitDistance;
    private com.google.android.material.materialswitch.MaterialSwitch switchPrivate;
    
    private Uri posterUri;
    private android.location.Location testingLocation;
    private android.location.Location selectedEventLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.et_event_name);
        etLocation = view.findViewById(R.id.et_event_location);
        etDescription = view.findViewById(R.id.et_event_description);
        etMaxAttendees = view.findViewById(R.id.et_max_attendees);
        etMaxWaitlist = view.findViewById(R.id.et_max_waitlist);
        btnConfirm = view.findViewById(R.id.btn_confirm);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnPreviewLocation = view.findViewById(R.id.btn_preview_location);
        
        btnRegStartDate = view.findViewById(R.id.btn_reg_start_date);
        btnRegEndDate = view.findViewById(R.id.btn_reg_end_date);
        btnEventStartDate = view.findViewById(R.id.btn_event_start_date);
        btnEventEndDate = view.findViewById(R.id.btn_event_end_date);

        btnRegStartDate.setOnClickListener(v -> showDateTimePicker(1));
        btnRegEndDate.setOnClickListener(v -> showDateTimePicker(2));
        btnEventStartDate.setOnClickListener(v -> showDateTimePicker(3));
        btnEventEndDate.setOnClickListener(v -> showDateTimePicker(4));

        switchGeo = view.findViewById(R.id.switch_geolocation);
        layoutLimitDistance = view.findViewById(R.id.layout_limit_distance);
        etLimitDistance = view.findViewById(R.id.et_limit_distance);
        switchPrivate = view.findViewById(R.id.switch_private_event);

        if (layoutLimitDistance != null) {
            layoutLimitDistance.setVisibility(View.GONE);
            switchGeo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                layoutLimitDistance.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        getParentFragmentManager().setFragmentResultListener(
                LocationPreviewDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    double lat = result.getDouble(LocationPreviewDialogFragment.RESULT_LATITUDE);
                    double lng = result.getDouble(LocationPreviewDialogFragment.RESULT_LONGITUDE);
                    selectedEventLocation = new android.location.Location("selected_event_location");
                    selectedEventLocation.setLatitude(lat);
                    selectedEventLocation.setLongitude(lng);
                }
        );

        etLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (testingLocation == null) {
                    selectedEventLocation = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> showCancelConfirmationDialog());
        btnPreviewLocation.setOnClickListener(v -> previewLocationOnMap());
        btnConfirm.setOnClickListener(v -> handleCreateEvent());
    }

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

    @VisibleForTesting
    public void setTestingLocation(double lat, double lng) {
        this.testingLocation = new android.location.Location("test");
        this.testingLocation.setLatitude(lat);
        this.testingLocation.setLongitude(lng);
        this.selectedEventLocation = this.testingLocation;
    }

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
                    case 1: regStartDate = selectedDateTime; btnRegStartDate.setText(dateTimeFormat.format(regStartDate)); break;
                    case 2: regEndDate = selectedDateTime; btnRegEndDate.setText(dateTimeFormat.format(regEndDate)); break;
                    case 3: eventStartDate = selectedDateTime; btnEventStartDate.setText(dateTimeFormat.format(eventStartDate)); break;
                    case 4: eventEndDate = selectedDateTime; btnEventEndDate.setText(dateTimeFormat.format(eventEndDate)); break;
                }
            });
            timePicker.show(getParentFragmentManager(), "TIME_PICKER");
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void handleCreateEvent() {
        String name = etName.getText().toString().trim();
        String locationStr = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        boolean isGeoRequired = switchGeo.isChecked();
        boolean isPrivate = switchPrivate != null && switchPrivate.isChecked();

        if (name.isEmpty()) { etName.setError("Name is required"); return; }
        if (locationStr.isEmpty()) { etLocation.setError("Location is required"); return; }
        if (description.isEmpty()) { etDescription.setError("Description is required"); return; }

        if (regStartDate == null || regEndDate == null || eventStartDate == null || eventEndDate == null) {
            Toast.makeText(requireContext(), "Please select all dates and times", Toast.LENGTH_SHORT).show();
            return;
        }

        double radius = 0.0;
        if (isGeoRequired) {
            try {
                radius = Double.parseDouble(etLimitDistance.getText().toString().trim());
                if (radius <= 0) { Toast.makeText(requireContext(), "Please enter a valid distance", Toast.LENGTH_SHORT).show(); return; }
            } catch (NumberFormatException e) { Toast.makeText(requireContext(), "Please enter a valid distance", Toast.LENGTH_SHORT).show(); return; }
        }

        Integer maxAttendees = null;
        if (!etMaxAttendees.getText().toString().trim().isEmpty()) {
            try { maxAttendees = Integer.parseInt(etMaxAttendees.getText().toString().trim()); }
            catch (NumberFormatException e) { etMaxAttendees.setError("Invalid number"); return; }
        }

        Integer maxWaitlist = null;
        if (!etMaxWaitlist.getText().toString().trim().isEmpty()) {
            try { maxWaitlist = Integer.parseInt(etMaxWaitlist.getText().toString().trim()); }
            catch (NumberFormatException e) { etMaxWaitlist.setError("Invalid number"); return; }
        }

        android.location.Location eventLocation = (testingLocation != null) ? testingLocation : selectedEventLocation;
        if (eventLocation == null) {
            Toast.makeText(requireContext(), "Preview the address and confirm the map location first", Toast.LENGTH_SHORT).show();
            return;
        }

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        byte[] photoBytes = (posterUri != null) ? uriToBytes(posterUri) : null;
        
        Integer finalMaxAttendees = maxAttendees;
        Integer finalMaxWaitlist = maxWaitlist;

        double finalRadius = radius;
        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            Organizer organizer = new Organizer(user.getDeviceId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());

            Event.create(name, description, organizer, locationStr, photoBytes, regStartDate, regEndDate, eventStartDate, eventEndDate, isGeoRequired, finalRadius, eventLocation, isPrivate)
                    .addOnSuccessListener(event -> {
                        event.setMaxAttendees(finalMaxAttendees);
                        event.setMaxWaitlist(finalMaxWaitlist);
                        event.save().addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                            try { getParentFragmentManager().popBackStack(); } catch (Exception ignored) {}
                        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show());
                    });
        });
    }

    private byte[] uriToBytes(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return baos.toByteArray();
        } catch (Exception e) { return null; }
    }

    private void showCancelConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancel_confirmation, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog).setView(dialogView).create();
        dialogView.findViewById(R.id.btn_dialog_yes).setOnClickListener(v -> { dialog.dismiss(); try { getParentFragmentManager().popBackStack(); } catch (Exception ignored) {} });
        dialogView.findViewById(R.id.btn_dialog_back).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void previewLocationOnMap() {
        String locationStr = etLocation.getText().toString().trim();
        if (locationStr.isEmpty()) {
            etLocation.setError("Location is required");
            return;
        }

        android.location.Location location = (testingLocation != null) ? testingLocation : getLocationFromAddress(locationStr);
        LocationPreviewDialogFragment
                .newInstance(locationStr, location)
                .show(getParentFragmentManager(), "location_preview");
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
        } catch (Exception ignored) {}
        return null;
    }

    public static class LocationPreviewDialogFragment extends DialogFragment {

        public static final String REQUEST_KEY = "location_preview_result";
        public static final String RESULT_LATITUDE = "result_latitude";
        public static final String RESULT_LONGITUDE = "result_longitude";
        private static final String ARG_LABEL = "arg_label";
        private static final String ARG_LAT = "arg_lat";
        private static final String ARG_LNG = "arg_lng";
        private static final String ARG_HAS_INITIAL = "arg_has_initial";

        private android.location.Location pendingLocation;

        public static LocationPreviewDialogFragment newInstance(String locationLabel, @Nullable android.location.Location initialLocation) {
            LocationPreviewDialogFragment fragment = new LocationPreviewDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_LABEL, locationLabel);
            args.putBoolean(ARG_HAS_INITIAL, initialLocation != null);
            if (initialLocation != null) {
                args.putDouble(ARG_LAT, initialLocation.getLatitude());
                args.putDouble(ARG_LNG, initialLocation.getLongitude());
            }
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.location_preview, null);
            AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                    .setView(dialogView)
                    .create();

            TextView title = dialogView.findViewById(R.id.tv_dialog_title);
            TextView message = dialogView.findViewById(R.id.tv_dialog_message);
            ImageView closeButton = dialogView.findViewById(R.id.iv_close);
            Button backButton = dialogView.findViewById(R.id.btn_dialog_back);
            Button confirmButton = dialogView.findViewById(R.id.btn_dialog_yes);

            Bundle args = requireArguments();
            String locationLabel = args.getString(ARG_LABEL, "Event location");
            boolean hasInitialLocation = args.getBoolean(ARG_HAS_INITIAL, false);
            LatLng initialLatLng = hasInitialLocation
                    ? new LatLng(args.getDouble(ARG_LAT), args.getDouble(ARG_LNG))
                    : new LatLng(53.5461, -113.4938);

            if (title != null) {
                title.setText("Location Preview");
            }
            if (message != null) {
                message.setText(hasInitialLocation
                        ? "Long press the map if you need to adjust the event location before confirming."
                        : "Address preview could not be found. Long press the map to place the event location manually.");
            }
            if (confirmButton != null) {
                confirmButton.setText("Use location");
            }

            pendingLocation = hasInitialLocation ? new android.location.Location("preview_initial") : null;
            if (pendingLocation != null) {
                pendingLocation.setLatitude(initialLatLng.latitude);
                pendingLocation.setLongitude(initialLatLng.longitude);
            }

            dialog.setOnShowListener(unused -> {
                View mapHost = dialogView.findViewById(R.id.map_preview_fragment);
                mapHost.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                            .findFragmentById(R.id.map_preview_fragment);
                    if (mapFragment == null) {
                        mapFragment = SupportMapFragment.newInstance();
                        getChildFragmentManager()
                                .beginTransaction()
                                .replace(R.id.map_preview_fragment, mapFragment)
                                .commitAllowingStateLoss();
                        getChildFragmentManager().executePendingTransactions();
                    }

                    mapFragment.getMapAsync(map -> {
                        map.getUiSettings().setZoomGesturesEnabled(true);
                        map.getUiSettings().setScrollGesturesEnabled(true);
                        map.getUiSettings().setRotateGesturesEnabled(true);
                        map.getUiSettings().setTiltGesturesEnabled(true);
                        map.getUiSettings().setCompassEnabled(true);

                        final Marker[] markerRef = new Marker[1];
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, hasInitialLocation ? 15f : 10f));

                        if (hasInitialLocation) {
                            markerRef[0] = map.addMarker(
                                    new MarkerOptions()
                                            .position(initialLatLng)
                                            .title(locationLabel)
                            );
                        }

                        map.setOnMapLongClickListener(latLng -> {
                            pendingLocation = new android.location.Location("selected_event_location");
                            pendingLocation.setLatitude(latLng.latitude);
                            pendingLocation.setLongitude(latLng.longitude);

                            if (markerRef[0] != null) {
                                markerRef[0].remove();
                            }
                            markerRef[0] = map.addMarker(
                                    new MarkerOptions()
                                            .position(latLng)
                                            .title(locationLabel)
                            );
                            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                        });
                    });
                });
            });

            if (closeButton != null) {
                closeButton.setOnClickListener(v -> dismiss());
            }
            if (backButton != null) {
                backButton.setOnClickListener(v -> dismiss());
            }
            if (confirmButton != null) {
                confirmButton.setOnClickListener(v -> {
                    if (pendingLocation == null) {
                        Toast.makeText(requireContext(), "Long press the map to choose a location", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Bundle result = new Bundle();
                    result.putDouble(RESULT_LATITUDE, pendingLocation.getLatitude());
                    result.putDouble(RESULT_LONGITUDE, pendingLocation.getLongitude());
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    dismiss();
                });
            }

            return dialog;
        }
    }
}
