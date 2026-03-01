package com.example.thevms.ui;

import android.app.DatePickerDialog;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Fragment for the main search/event listing screen.
 * Allows entrants to filter events by name and start time.
 */
public class SearchFragment extends Fragment {

    private EditText searchEditText;
    private ImageView clearSearchIcon;
    private Button filterDateBtn;
    private Button clearFiltersBtn;
    private TextView resultsCountText;
    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();

    private String nameFilter = "";
    private Date dateFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchEditText = view.findViewById(R.id.search_edit_text);
        clearSearchIcon = view.findViewById(R.id.clear_search);
        filterDateBtn = view.findViewById(R.id.filter_date_btn);
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

        filterDateBtn.setOnClickListener(v -> showDatePicker());

        clearFiltersBtn.setOnClickListener(v -> {
            dateFilter = null;
            filterDateBtn.setText("Select Date");
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
                    // Log or handle error for individual document parsing
                }
            }
            applyFilters();
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (dateFilter != null) {
            calendar.setTime(dateFilter);
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            dateFilter = selected.getTime();
            
            filterDateBtn.setText(android.text.format.DateFormat.getDateFormat(getContext()).format(dateFilter));
            clearFiltersBtn.setVisibility(View.VISIBLE);
            applyFilters();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void applyFilters() {
        filteredEvents.clear();
        for (Event event : allEvents) {
            String eventName = event.getName() != null ? event.getName() : "";
            boolean matchesName = eventName.toLowerCase().contains(nameFilter);
            boolean matchesDate = true;

            if (dateFilter != null) {
                if (event.getEventStartTime() != null) {
                    Calendar eventCal = Calendar.getInstance();
                    eventCal.setTime(event.getEventStartTime());
                    Calendar filterCal = Calendar.getInstance();
                    filterCal.setTime(dateFilter);
                    
                    matchesDate = eventCal.get(Calendar.YEAR) == filterCal.get(Calendar.YEAR) &&
                                 eventCal.get(Calendar.DAY_OF_YEAR) == filterCal.get(Calendar.DAY_OF_YEAR);
                } else {
                    matchesDate = false;
                }
            }

            if (matchesName && matchesDate) {
                filteredEvents.add(event);
            }
        }
        eventAdapter.setEvents(filteredEvents);
        resultsCountText.setText(filteredEvents.size() + " results");
    }
}
