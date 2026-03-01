package com.example.thevms.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
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
 * Allows entrants to filter events by name, date range, and time range.
 */
public class SearchFragment extends Fragment {

    private EditText searchEditText;
    private ImageView clearSearchIcon;
    private Button filterDateRangeBtn;
    private Button filterTimeRangeBtn;
    private Button clearFiltersBtn;
    private TextView resultsCountText;
    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();

    private String nameFilter = "";
    private Long startDateFilter = null;
    private Long endDateFilter = null;
    private Integer startTimeHour = null;
    private Integer startTimeMinute = null;
    private Integer endTimeHour = null;
    private Integer endTimeMinute = null;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchEditText = view.findViewById(R.id.search_edit_text);
        clearSearchIcon = view.findViewById(R.id.clear_search);
        filterDateRangeBtn = view.findViewById(R.id.filter_date_range_btn);
        filterTimeRangeBtn = view.findViewById(R.id.filter_time_range_btn);
        clearFiltersBtn = view.findViewById(R.id.clear_filters_btn);
        resultsCountText = view.findViewById(R.id.results_count_text);
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);

        eventAdapter = new EventAdapter();
        eventsRecyclerView.setAdapter(eventAdapter);

        setupListeners();
        fetchEvents();

        return view;
    }

    private void setupListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameFilter = s.toString().toLowerCase();
                clearSearchIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchIcon.setOnClickListener(v -> searchEditText.setText(""));

        filterDateRangeBtn.setOnClickListener(v -> showDateRangePicker());
        filterTimeRangeBtn.setOnClickListener(v -> showTimeRangePicker());

        clearFiltersBtn.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            startTimeHour = null;
            startTimeMinute = null;
            endTimeHour = null;
            endTimeMinute = null;
            filterDateRangeBtn.setText("Date Range");
            filterTimeRangeBtn.setText("Time Range");
            clearFiltersBtn.setVisibility(View.GONE);
            applyFilters();
        });
    }

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

    private void applyFilters() {
        filteredEvents.clear();
        for (Event event : allEvents) {
            String eventName = event.getName() != null ? event.getName() : "";
            boolean matchesName = eventName.toLowerCase().contains(nameFilter);
            
            boolean matchesDate = true;
            if (startDateFilter != null && endDateFilter != null && event.getEventStartTime() != null) {
                long eventTime = event.getEventStartTime().getTime();
                // Normalize event time to start of day for accurate comparison if needed, 
                // but usually ranges cover the whole day.
                matchesDate = eventTime >= startDateFilter && eventTime <= endDateFilter + 86400000; // +1 day
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

            if (matchesName && matchesDate && matchesTime) {
                filteredEvents.add(event);
            }
        }
        eventAdapter.setEvents(filteredEvents);
        resultsCountText.setText(filteredEvents.size() + " results");
    }
}
