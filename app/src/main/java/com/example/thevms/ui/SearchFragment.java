package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.Event.EventAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for the main search/event listing screen.
 * Allows users to browse and filter events by name, date range, time range, and capacity.
 */
public class SearchFragment extends Fragment {

    private EditText searchEditText;
    private ImageView clearSearchIcon;
    private Button filterDateRangeBtn;
    private Button filterTimeRangeBtn;
    private Button filterCapacityBtn;
    private Button clearFiltersBtn;
    private TextView resultsCountText;
    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private ImageButton btnAdminBurger;

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
        resultsCountText = view.findViewById(R.id.results_count_text);
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        btnAdminBurger = view.findViewById(R.id.btn_admin_burger);

        eventAdapter = new EventAdapter();
        eventsRecyclerView.setAdapter(eventAdapter);

        setupListeners();
        checkUserRoleAndFetchEvents();

        return view;
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

        clearFiltersBtn.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            startTimeHour = null;
            startTimeMinute = null;
            endTimeHour = null;
            endTimeMinute = null;
            targetCapacityFilter = null;
            filterDateRangeBtn.setText("Date Range");
            filterTimeRangeBtn.setText("Time Range");
            filterCapacityBtn.setText("Capacity");
            clearFiltersBtn.setVisibility(View.GONE);
            applyFilters();
        });
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
        filteredEvents.clear();
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
                filteredEvents.add(event);
            }
        }
        eventAdapter.setEvents(filteredEvents);
        resultsCountText.setText(filteredEvents.size() + " results");
    }
}
