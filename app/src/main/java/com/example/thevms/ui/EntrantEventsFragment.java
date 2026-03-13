package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.Event.EventAdapter;
import com.example.thevms.ui.Event.OrganizerEventAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EntrantEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar loadingBar;
    private TextView emptyText;
    private DatabaseHandler dbHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrant_events, container, false);

        recyclerView = view.findViewById(R.id.rv_my_events);
        loadingBar = view.findViewById(R.id.pb_loading);
        emptyText = view.findViewById(R.id.tv_no_events);
        dbHandler = new DatabaseHandler();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadDashboardBasedOnRole();

        return view;
    }

    private void loadDashboardBasedOnRole() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);
        if (emptyText != null) emptyText.setVisibility(View.GONE);

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            UserRole role = entrant.getRole();
            Log.d("HistoryTab", "Loading view for role: " + role);
            
            if (role == UserRole.ADMIN) {
                setupAdminView();
            } else if (role == UserRole.ORGANIZER) {
                setupOrganizerView(deviceId);
            } else {
                setupEntrantView(entrant);
            }
        }).addOnFailureListener(e -> handleFailure("Error identifying user profile."));
    }

    private void setupEntrantView(Entrant entrant) {
        EventAdapter adapter = new EventAdapter();
        recyclerView.setAdapter(adapter);

        entrant.getRegisteredEvents().addOnSuccessListener(events -> {
            Log.d("HistoryTab", "Loaded " + (events != null ? events.size() : 0) + " signed up events");
            updateUI(events, "You haven't joined any events yet.");
            adapter.setEvents(events);
        }).addOnFailureListener(e -> handleFailure("Query failed. You might need to create a Firestore Index. Check Logcat."));
    }

    private void setupOrganizerView(String deviceId) {
        OrganizerEventAdapter adapter = new OrganizerEventAdapter();
        recyclerView.setAdapter(adapter);

        dbHandler.getEventsByOrganizer(deviceId).addOnSuccessListener(queryDocumentSnapshots -> {
            List<Event> events = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                events.add(Event.fromDoc(doc));
            }
            updateUI(events, "You haven't created any events yet.");
            adapter.setEvents(events);
        }).addOnFailureListener(e -> handleFailure("Error fetching your events."));
    }

    private void setupAdminView() {
        EventAdapter adapter = new EventAdapter();
        adapter.setAdmin(true);
        recyclerView.setAdapter(adapter);

        dbHandler.getAllEvents().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Event> events = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                events.add(Event.fromDoc(doc));
            }
            updateUI(events, "No events exist in the system.");
            adapter.setEvents(events);
        }).addOnFailureListener(e -> handleFailure("Admin access failed."));
    }

    private void updateUI(List<Event> events, String emptyMessage) {
        if (loadingBar != null) loadingBar.setVisibility(View.GONE);
        if (events == null || events.isEmpty()) {
            if (emptyText != null) {
                emptyText.setText(emptyMessage);
                emptyText.setVisibility(View.VISIBLE);
            }
        } else {
            if (emptyText != null) emptyText.setVisibility(View.GONE);
        }
    }

    private void handleFailure(String message) {
        Log.e("HistoryTab", message);
        if (loadingBar != null) loadingBar.setVisibility(View.GONE);
        if (emptyText != null) {
            emptyText.setText(message);
            emptyText.setVisibility(View.VISIBLE);
        }
    }
}
