package com.example.thevms.ui.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.ui.Event.EventAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar loadingSpinner;
    private EventAdapter adapter;
    private final List<Event> events = new ArrayList<>();
    private DatabaseHandler dbHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_events, container, false);

        recyclerView = view.findViewById(R.id.events_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        loadingSpinner = view.findViewById(R.id.loading_spinner);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventAdapter();
        adapter.setAdmin(true); // This enables the remove button

        // Need a way to tell the adapter to hide Join/Leave buttons
        adapter.setManagementMode(true);
        recyclerView.setAdapter(adapter);

        dbHandler = new DatabaseHandler();
        loadEvents();

        return view;
    }

    private void setLoading(boolean isLoading) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
        } else {
            updateUI();
        }
    }

    private void loadEvents() {
        setLoading(true);
        dbHandler.getAllEvents().addOnSuccessListener(queryDocumentSnapshots -> {
            events.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    Event event = Event.fromDoc(doc);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    Log.e("AdminEventsFragment", "Error parsing event", e);
                }
            }
            setLoading(false);
        }).addOnFailureListener(e -> {
            setLoading(false);
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.admin_events_load_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        adapter.setEvents(events);
        if (events.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }
}
