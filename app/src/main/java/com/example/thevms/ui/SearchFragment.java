package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.Event.EventAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for the main search/event listing screen.
 * Allows users to browse and filter events by name, date range, time range, and capacity.
 */
public class SearchFragment extends Fragment implements OnMapReadyCallback {
    private static final LatLng DEFAULT_MAP_CENTER = new LatLng(53.5461, -113.4938);
    private static final float DEFAULT_MAP_ZOOM = 11.5f;
    private static final float DISTANCE_LABEL_ZOOM_THRESHOLD = 13.5f;
    private static final float INITIAL_FOCUS_ZOOM = 12.8f;
    private static final double MAX_AUTO_FIT_LAT_SPAN = 0.30d;
    private static final double MAX_AUTO_FIT_LNG_SPAN = 0.30d;
    private static final int DEFAULT_MAP_TOP_PADDING_DP = 148;
    private static final int DEFAULT_MAP_BOTTOM_PADDING_DP = 220;
    private static final int DEFAULT_SHEET_PEEK_HEIGHT_DP = 200;
    private static final int SELECTED_SHEET_PEEK_HEIGHT_DP = 420;
    private static final int RECENTER_BUTTON_CLEARANCE_DP = 132;

    private EditText searchEditText;
    private ImageView clearSearchIcon;
    private Button filterDateRangeBtn;
    private Button filterTimeRangeBtn;
    private Button filterCapacityBtn;
    private Button clearFiltersBtn;
    private MaterialButton retryLocationBtn;
    private TextView resultsCountText;
    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private ImageButton btnAdminBurger;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation;
    private Marker userLocationMarker;
    private Circle userLocationHalo;
    private LocationCallback liveLocationCallback;
    private final List<Marker> eventMarkers = new ArrayList<>();
    private Runnable pendingLocationPermissionGrantedAction;
    private Runnable pendingLocationPermissionDeniedAction;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean granted = false;
                for (Boolean value : result.values()) {
                    if (Boolean.TRUE.equals(value)) {
                        granted = true;
                        break;
                    }
                }
                Runnable action = granted ? pendingLocationPermissionGrantedAction : pendingLocationPermissionDeniedAction;
                pendingLocationPermissionGrantedAction = null;
                pendingLocationPermissionDeniedAction = null;
                if (action != null) {
                    action.run();
                }
            }
    );

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();

    private String nameFilter = "";
    private Long startDateFilter = null;
    private Long endDateFilter = null;
    private Integer startTimeHour = null;
    private Integer startTimeMinute = null;
    private Integer endTimeHour = null;
    private Integer endTimeMinute = null;
    private Integer targetCapacityFilter = null;

    private boolean isAdmin = false;
    private String selectedMarkerKey = null;
    private boolean hasCenteredMapOnce = false;
    private int lastRenderedMarkerZoomBucket = Integer.MIN_VALUE;
    private boolean lastRenderedDistanceMode = false;
    private boolean preserveMapViewportOnNextFilterApply = false;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchEditText = view.findViewById(R.id.search_edit_text);
        clearSearchIcon = view.findViewById(R.id.clear_search);
        filterDateRangeBtn = view.findViewById(R.id.filter_date_range_btn);
        filterTimeRangeBtn = view.findViewById(R.id.filter_time_range_btn);
        filterCapacityBtn = view.findViewById(R.id.filter_capacity_btn);
        clearFiltersBtn = view.findViewById(R.id.clear_filters_btn);
        retryLocationBtn = view.findViewById(R.id.btn_retry_location);
        resultsCountText = view.findViewById(R.id.results_count_text);
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        btnAdminBurger = view.findViewById(R.id.btn_admin_burger);
        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottom_sheet));
        bottomSheetBehavior.setPeekHeight(dpToPx(DEFAULT_SHEET_PEEK_HEIGHT_DP), false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        eventAdapter = new EventAdapter();
        eventAdapter.setLocationPermissionRequester((requiredForJoin, onGranted, onDenied) -> {
            if (hasLocationPermission()) {
                onGranted.run();
                return;
            }
            pendingLocationPermissionGrantedAction = onGranted;
            pendingLocationPermissionDeniedAction = onDenied;
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        });
        eventsRecyclerView.setAdapter(eventAdapter);

        setupMap();
        setupListeners();
        checkUserRoleAndFetchEvents();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ensureMapLocationEnabled();
    }

    /**
     * Initializes listeners for search input and filter buttons.
     */
    private void setupListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameFilter = s.toString().toLowerCase();
                clearSearchIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        clearSearchIcon.setOnClickListener(v -> searchEditText.setText(""));

        filterDateRangeBtn.setOnClickListener(v -> showDateRangePicker());
        filterTimeRangeBtn.setOnClickListener(v -> showTimeRangePicker());
        filterCapacityBtn.setOnClickListener(v -> showCapacityPickerDialog());
        retryLocationBtn.setOnClickListener(v -> {
            if (currentUserLocation != null && googleMap != null) {
                centerMapOnUserLocation();
            } else {
                retryLocationRequest();
                refreshMap(new ArrayList<>(filteredEvents));
            }
        });

        clearFiltersBtn.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            startTimeHour = null;
            startTimeMinute = null;
            endTimeHour = null;
            endTimeMinute = null;
            targetCapacityFilter = null;
            selectedMarkerKey = null;
            filterDateRangeBtn.setText("Date Range");
            filterTimeRangeBtn.setText("Time Range");
            filterCapacityBtn.setText("Capacity");
            clearFiltersBtn.setVisibility(View.GONE);
            applyDefaultMapViewportState();
            applyFilters();
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commitNow();
        }
        mapFragment.getMapAsync(this);
    }

    private void ensureMapLocationEnabled() {
        if (googleMap == null) {
            return;
        }
        showLocationButton();
        if (hasLocationPermission()) {
            enableUserLocation();
            return;
        }
        pendingLocationPermissionGrantedAction = this::enableUserLocation;
        pendingLocationPermissionDeniedAction = this::showLocationButton;
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void retryLocationRequest() {
        if (!hasLocationPermission()) {
            ensureMapLocationEnabled();
            return;
        }
        if (!isSystemLocationEnabled()) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        enableUserLocation();
    }

    @SuppressLint("MissingPermission")
    private void enableUserLocation() {
        if (googleMap == null || !hasLocationPermission()) {
            return;
        }
        if (!isSystemLocationEnabled()) {
            showLocationButton();
            return;
        }
        fetchUserLocationAndRender();
    }

    private void updateCurrentUserLocation(@NonNull Location location) {
        currentUserLocation = location;
        updateRecenterButtonState();
        refreshMap(new ArrayList<>(filteredEvents));
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocationAndRender() {
        if (!hasLocationPermission()) {
            showLocationButton();
            return;
        }

        if (liveLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(liveLocationCallback);
            liveLocationCallback = null;
        }

        Location fallbackLocation = getBestLastKnownLocation();
        if (fallbackLocation != null) {
            updateCurrentUserLocation(fallbackLocation);
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateCurrentUserLocation(location);
                        return;
                    }
                    requestFreshLocationUpdates();
                })
                .addOnFailureListener(e -> requestFreshLocationUpdates());
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocationUpdates() {
        if (!hasLocationPermission()) {
            showLocationButton();
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(5)
                .build();

        liveLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    fusedLocationClient.removeLocationUpdates(this);
                    liveLocationCallback = null;
                    updateCurrentUserLocation(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, liveLocationCallback, Looper.getMainLooper());

        if (currentUserLocation == null) {
            showLocationButton();
        }
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private Location getBestLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }

        LocationManager locationManager = (LocationManager) requireContext()
                .getSystemService(android.content.Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLocation == null) {
            return networkLocation;
        }
        if (networkLocation == null) {
            return gpsLocation;
        }
        return gpsLocation.getTime() >= networkLocation.getTime() ? gpsLocation : networkLocation;
    }

    private boolean isSystemLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void drawUserLocation(@NonNull Location location) {
        if (googleMap == null) {
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userLocationHalo != null) {
            userLocationHalo.remove();
        }
        userLocationHalo = googleMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(24)
                .strokeWidth(0f)
                .fillColor(0x332196F3));

        BitmapDescriptor icon = createBlueDotIcon();
        if (userLocationMarker == null) {
            userLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .zIndex(1000f)
                    .icon(icon));
        } else {
            userLocationMarker.setPosition(latLng);
            userLocationMarker.setIcon(icon);
        }
    }

    private void showLocationButton() {
        if (retryLocationBtn != null) {
            retryLocationBtn.setVisibility(View.VISIBLE);
        }
    }

    private void updateRecenterButtonState() {
        if (retryLocationBtn == null) {
            return;
        }
        retryLocationBtn.setIconResource(currentUserLocation != null
                ? android.R.drawable.ic_menu_mylocation
                : android.R.drawable.ic_popup_sync);
    }

    /**
     * Displays a dialog to input the target event capacity for filtering (+/- 5).
     */
    private void showCapacityPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Target Capacity (+/- 5)");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (targetCapacityFilter != null) {
            input.setText(String.valueOf(targetCapacityFilter));
        }
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText().toString();
            if (!value.isEmpty()) {
                targetCapacityFilter = Integer.parseInt(value);
                filterCapacityBtn.setText("Cap: " + targetCapacityFilter + "±5");
                clearFiltersBtn.setVisibility(View.VISIBLE);
                applyFilters();
            } else {
                targetCapacityFilter = null;
                filterCapacityBtn.setText("Capacity");
                applyFilters();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Testing helper to manually set filters and bypass picker UI.
     *
     * @param startDate Long representing start date in millis.
     * @param endDate   Long representing end date in millis.
     * @param startHour Integer start hour.
     * @param startMin  Integer start minute.
     * @param endHour   Integer end hour.
     * @param endMin    Integer end minute.
     * @param capacity  Integer target capacity.
     */
    @VisibleForTesting
    public void setTestingFilters(Long startDate, Long endDate, Integer startHour, Integer startMin, Integer endHour, Integer endMin, Integer capacity) {
        this.startDateFilter = startDate;
        this.endDateFilter = endDate;
        this.startTimeHour = startHour;
        this.startTimeMinute = startMin;
        this.endTimeHour = endHour;
        this.endTimeMinute = endMin;
        this.targetCapacityFilter = capacity;

        if (startDate != null && endDate != null) {
            filterDateRangeBtn.setText(dateFormat.format(new Date(startDate)) + " - " + dateFormat.format(new Date(endDate)));
        }
        if (startHour != null && endHour != null) {
            filterTimeRangeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin));
        }
        if (capacity != null) {
            filterCapacityBtn.setText("Cap: " + capacity + "±5");
        }

        clearFiltersBtn.setVisibility(View.VISIBLE);
        applyFilters();
    }

    /**
     * Testing helper to programmatically expand the bottom sheet,
     * avoiding inconsistent Espresso swipe/scroll gestures.
     */
    @VisibleForTesting
    public void expandBottomSheet() {
        View bottomSheet = getView().findViewById(R.id.bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    /**
     * Checks the user's role to determine UI visibility and then fetches the event list.
     */
    private void checkUserRoleAndFetchEvents() {
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            this.isAdmin = (user.getRole() == UserRole.ADMIN);
            eventAdapter.setAdmin(isAdmin);

            if (isAdmin) {
                btnAdminBurger.setVisibility(View.VISIBLE);
                btnAdminBurger.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openAdminDrawer();
                    }
                });
            }

            fetchEvents();
        }).addOnFailureListener(e -> {
            Log.e("SearchFragment", "Error verifying user role", e);
            fetchEvents();
        });
    }

    /**
     * Fetches all events from the database and triggers the filtering logic.
     */
    private void fetchEvents() {
        DatabaseHandler dbHandler = new DatabaseHandler();
        dbHandler.getAllEvents().addOnSuccessListener(queryDocumentSnapshots -> {
            allEvents.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    Event event = Event.fromDoc(doc);
                    if (event != null) {
                        allEvents.add(event);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            applyFilters();
        });
    }

    private boolean hasLocationPermission() {
        return androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Displays a Material Date Range Picker for filtering events.
     */
    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select date range")
                .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            startDateFilter = selection.first;
            endDateFilter = selection.second;
            filterDateRangeBtn.setText(dateFormat.format(new Date(startDateFilter)) + " - " + dateFormat.format(new Date(endDateFilter)));
            clearFiltersBtn.setVisibility(View.VISIBLE);
            applyFilters();
        });
        dateRangePicker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
    }

    /**
     * Displays sequential Time Pickers to select a start and end time range for filtering.
     */
    private void showTimeRangePicker() {
        // Simplified: Pick start time then end time
        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            startTimeHour = hourOfDay;
            startTimeMinute = minute;

            new TimePickerDialog(getContext(), (view1, hourOfDay1, minute1) -> {
                endTimeHour = hourOfDay1;
                endTimeMinute = minute1;
                filterTimeRangeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startTimeHour, startTimeMinute, endTimeHour, endTimeMinute));
                clearFiltersBtn.setVisibility(View.VISIBLE);
                applyFilters();
            }, hourOfDay, minute, true).show();

        }, 12, 0, true).show();
    }

    /**
     * Applies the current filters (name, date range, time range, capacity) to the list of all events.
     * Also handles visibility of events based on registration window.
     */
    private void applyFilters() {
        List<Event> baseFilteredEvents = new ArrayList<>();
        long now = System.currentTimeMillis();
        long oneDayMillis = 86400000L;

        for (Event event : allEvents) {
            // US 02.03.01: Private events should NEVER be visible through search features
            if (event.isPrivate()) {
                continue;
            }

            // 1. Don't show if registration start is more than a day away
            if (event.getRegistrationStartTime() != null) {
                if (event.getRegistrationStartTime().getTime() - now > oneDayMillis) {
                    continue;
                }
            }
            // 2. Don't show if registration end was more than a day ago
            if (event.getRegistrationEndTime() != null) {
                if (now - event.getRegistrationEndTime().getTime() > oneDayMillis) {
                    continue;
                }
            }

            String eventName = event.getName() != null ? event.getName() : "";
            boolean matchesName = eventName.toLowerCase().contains(nameFilter);

            boolean matchesDate = true;
            if (startDateFilter != null && endDateFilter != null && event.getEventStartTime() != null) {
                long eventTime = event.getEventStartTime().getTime();
                matchesDate = eventTime >= startDateFilter && eventTime <= endDateFilter + oneDayMillis;
            }

            boolean matchesTime = true;
            if (startTimeHour != null && event.getEventStartTime() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(event.getEventStartTime());
                int evHour = cal.get(Calendar.HOUR_OF_DAY);
                int evMin = cal.get(Calendar.MINUTE);

                int startTotal = startTimeHour * 60 + startTimeMinute;
                int endTotal = endTimeHour * 60 + endTimeMinute;
                int evTotal = evHour * 60 + evMin;

                matchesTime = evTotal >= startTotal && evTotal <= endTotal;
            }

            boolean matchesCapacity = true;
            if (targetCapacityFilter != null) {
                Integer maxAttendees = event.getMaxAttendees();
                if (maxAttendees == null || maxAttendees < targetCapacityFilter - 5 || maxAttendees > targetCapacityFilter + 5) {
                    matchesCapacity = false;
                }
            }

            if (matchesName && matchesDate && matchesTime && matchesCapacity) {
                baseFilteredEvents.add(event);
            }
        }

        if (selectedMarkerKey != null && !containsMarkerKey(baseFilteredEvents, selectedMarkerKey)) {
            selectedMarkerKey = null;
        }

        filteredEvents.clear();
        if (selectedMarkerKey == null) {
            filteredEvents.addAll(baseFilteredEvents);
        } else {
            for (Event event : baseFilteredEvents) {
                if (selectedMarkerKey.equals(getMarkerKey(event))) {
                    filteredEvents.add(event);
                }
            }
            if (eventsRecyclerView != null) {
                eventsRecyclerView.scrollToPosition(0);
            }
        }

        eventAdapter.setEvents(filteredEvents);
        resultsCountText.setText(filteredEvents.size() + " results");
        clearFiltersBtn.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
        boolean shouldAdjustCamera = !preserveMapViewportOnNextFilterApply;
        preserveMapViewportOnNextFilterApply = false;
        refreshMap(baseFilteredEvents, shouldAdjustCamera);
    }

    private boolean hasActiveFilters() {
        return !nameFilter.isEmpty()
                || startDateFilter != null
                || endDateFilter != null
                || startTimeHour != null
                || endTimeHour != null
                || targetCapacityFilter != null
                || selectedMarkerKey != null;
    }

    private boolean containsMarkerKey(List<Event> events, String key) {
        for (Event event : events) {
            if (key.equals(getMarkerKey(event))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private String getMarkerKey(Event event) {
        if (event.getGeoLocation() == null) {
            return null;
        }
        return String.format(Locale.US, "%.5f,%.5f",
                event.getGeoLocation().getLatitude(),
                event.getGeoLocation().getLongitude());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM));
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        googleMap.setBuildingsEnabled(false);
        applyDefaultMapViewportState();
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (!(tag instanceof String)) {
                return false;
            }
            selectedMarkerKey = (String) tag;
            applyFilters();
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setPeekHeight(dpToPx(SELECTED_SHEET_PEEK_HEIGHT_DP), true);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            centerMapOnSelectedMarker(marker);
            return true;
        });
        googleMap.setOnMapClickListener(latLng -> {
            if (selectedMarkerKey != null) {
                selectedMarkerKey = null;
                preserveMapViewportOnNextFilterApply = true;
                applyDefaultMapViewportState();
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setPeekHeight(dpToPx(DEFAULT_SHEET_PEEK_HEIGHT_DP), true);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                applyFilters();
            }
        });
        googleMap.setOnCameraIdleListener(() -> {
            if (shouldRefreshMarkersForCameraChange()) {
                refreshMap(new ArrayList<>(filteredEvents));
            }
        });
        ensureMapLocationEnabled();
        updateRecenterButtonState();
        refreshMap(new ArrayList<>(filteredEvents), true);
    }

    private void refreshMap(List<Event> mapEvents) {
        refreshMap(mapEvents, false);
    }

    private void refreshMap(List<Event> mapEvents, boolean shouldAdjustCamera) {
        if (googleMap == null) {
            return;
        }

        lastRenderedMarkerZoomBucket = getMarkerZoomBucket();
        lastRenderedDistanceMode = shouldShowDistanceLabel();

        clearEventMarkers();
        Map<String, List<Event>> buckets = new LinkedHashMap<>();
        for (Event event : mapEvents) {
            String key = getMarkerKey(event);
            if (key == null) {
                continue;
            }
            buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(event);
        }

        if (buckets.isEmpty()) {
            if (shouldAdjustCamera || !hasCenteredMapOnce) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM));
                hasCenteredMapOnce = true;
            }
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        List<LatLng> markerPositions = new ArrayList<>();
        Marker selectedMarker = null;

        for (Map.Entry<String, List<Event>> entry : buckets.entrySet()) {
            Event firstEvent = entry.getValue().get(0);
            LatLng position = new LatLng(
                    firstEvent.getGeoLocation().getLatitude(),
                    firstEvent.getGeoLocation().getLongitude()
            );
            boolean isSelected = entry.getKey().equals(selectedMarkerKey);
            String markerLabel = buildMarkerLabel(firstEvent, entry.getValue().size());

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .anchor(0.5f, 0.5f)
                    .icon(createMarkerIcon(isSelected, markerLabel)));
            if (marker != null) {
                eventMarkers.add(marker);
                marker.setTag(entry.getKey());
                if (isSelected) {
                    selectedMarker = marker;
                }
            }
            markerPositions.add(position);
            boundsBuilder.include(position);
        }

        if (currentUserLocation != null) {
            drawUserLocation(currentUserLocation);
        }

        if (!shouldAdjustCamera && hasCenteredMapOnce) {
            return;
        }

        if (selectedMarker != null) {
            centerMapOnSelectedMarker(selectedMarker);
        } else if (buckets.size() == 1) {
            Event event = buckets.values().iterator().next().get(0);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(event.getGeoLocation().getLatitude(), event.getGeoLocation().getLongitude()),
                INITIAL_FOCUS_ZOOM
            ));
        } else if (shouldUseFocusedInitialViewport(markerPositions)) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    markerPositions.get(0),
                    INITIAL_FOCUS_ZOOM
            ));
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), dpToPx(72)));
        }
        hasCenteredMapOnce = true;
    }

    private void clearEventMarkers() {
        for (Marker marker : eventMarkers) {
            marker.remove();
        }
        eventMarkers.clear();
    }

    private boolean shouldUseFocusedInitialViewport(@NonNull List<LatLng> positions) {
        if (positions.size() < 2) {
            return false;
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        for (LatLng position : positions) {
            minLat = Math.min(minLat, position.latitude);
            maxLat = Math.max(maxLat, position.latitude);
            minLng = Math.min(minLng, position.longitude);
            maxLng = Math.max(maxLng, position.longitude);
        }

        return (maxLat - minLat) > MAX_AUTO_FIT_LAT_SPAN
                || (maxLng - minLng) > MAX_AUTO_FIT_LNG_SPAN;
    }

    private void applyDefaultMapViewportState() {
        if (googleMap == null) {
            return;
        }
        googleMap.setPadding(0, dpToPx(DEFAULT_MAP_TOP_PADDING_DP), 0, dpToPx(DEFAULT_MAP_BOTTOM_PADDING_DP));
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setPeekHeight(dpToPx(DEFAULT_SHEET_PEEK_HEIGHT_DP), true);
        }
    }

    private void applySelectedMapViewportState() {
        if (googleMap == null) {
            return;
        }
        googleMap.setPadding(
                0,
                dpToPx(DEFAULT_MAP_TOP_PADDING_DP),
                0,
                dpToPx(SELECTED_SHEET_PEEK_HEIGHT_DP + RECENTER_BUTTON_CLEARANCE_DP)
        );
    }

    private void centerMapOnUserLocation() {
        if (googleMap == null || currentUserLocation == null) {
            return;
        }
        applyDefaultMapViewportState();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude()),
                Math.max(googleMap.getCameraPosition().zoom, 14f)
        ));
    }

    private void centerMapOnSelectedMarker(@NonNull Marker marker) {
        if (googleMap == null) {
            return;
        }
        applySelectedMapViewportState();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                marker.getPosition(),
                Math.max(googleMap.getCameraPosition().zoom, 14f)
        ));
    }

    private String buildMarkerLabel(Event event, int count) {
        if (shouldShowDistanceLabel() && currentUserLocation != null && event.getGeoLocation() != null) {
            float distanceMeters = currentUserLocation.distanceTo(event.getGeoLocation());
            float distanceKm = distanceMeters / 1000f;
            if (distanceKm < 10f) {
                return String.format(Locale.US, "%.1fkm", distanceKm);
            }
            return String.format(Locale.US, "%.0fkm", distanceKm);
        }
        return String.valueOf(count);
    }

    private boolean shouldShowDistanceLabel() {
        return googleMap != null
                && googleMap.getCameraPosition() != null
                && googleMap.getCameraPosition().zoom >= DISTANCE_LABEL_ZOOM_THRESHOLD;
    }

    private boolean shouldRefreshMarkersForCameraChange() {
        if (googleMap == null || googleMap.getCameraPosition() == null) {
            return false;
        }
        return lastRenderedDistanceMode != shouldShowDistanceLabel()
                || lastRenderedMarkerZoomBucket != getMarkerZoomBucket();
    }

    private int getMarkerZoomBucket() {
        if (googleMap == null || googleMap.getCameraPosition() == null) {
            return 0;
        }
        return Math.round(googleMap.getCameraPosition().zoom * 2f);
    }

    private BitmapDescriptor createMarkerIcon(boolean selected, String label) {
        float zoom = googleMap != null && googleMap.getCameraPosition() != null
                ? googleMap.getCameraPosition().zoom
                : DEFAULT_MAP_ZOOM;
        float zoomProgress = Math.max(0f, Math.min(1f, (zoom - 10f) / 6f));
        boolean isDistanceLabel = label.endsWith("km");
        int textSizeDp = Math.round((isDistanceLabel ? 12.5f : 13.5f) + (2f * zoomProgress));
        int horizontalPadding = dpToPx(Math.round((isDistanceLabel ? 9f : 7f) + (3f * zoomProgress)));
        int height = dpToPx(Math.round(32f + (10f * zoomProgress)));

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(selected ? Color.WHITE : 0xFF2B2B2B);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dpToPx(textSizeDp));

        int width = Math.max(height, Math.round(textPaint.measureText(label)) + horizontalPadding * 2);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x20000000);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(selected ? 0xFF171717 : 0xFFFFFFFF);

        RectF shadowRect = new RectF(dpToPx(1), dpToPx(2), width - dpToPx(1), height - dpToPx(1));
        RectF fillRect = new RectF(0, 0, width - dpToPx(2), height - dpToPx(3));
        float radius = height / 2f;

        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint);
        canvas.drawRoundRect(fillRect, radius, radius, fillPaint);

        float textX = width / 2f;
        float textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(label, textX, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private BitmapDescriptor createBlueDotIcon() {
        int size = dpToPx(24);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerPaint.setColor(0xFFFFFFFF);

        Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setColor(0xFF2196F3);

        float center = size / 2f;
        canvas.drawCircle(center, center, dpToPx(10), outerPaint);
        canvas.drawCircle(center, center, dpToPx(6), innerPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
