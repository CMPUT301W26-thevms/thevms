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
import com.example.thevms.ui.Event.EventAdapter;

import java.util.List;

/**
 * Fragment for displaying events the current user has registered for.
 * This view is the same for all users (Entrants, Organizers, Admins)
 * and focuses on their history as an event participant.
 */
public class EntrantEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar loadingBar;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrant_events, container, false);

        recyclerView = view.findViewById(R.id.rv_my_events);
        loadingBar = view.findViewById(R.id.pb_loading);
        emptyText = view.findViewById(R.id.tv_no_events);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadRegisteredEvents();

        return view;
    }

    /**
     * Loads the events the current device user has registered for.
     */
    private void loadRegisteredEvents() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);
        if (emptyText != null) emptyText.setVisibility(View.GONE);

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            Log.d("HistoryTab", "Loading registered events for: " + deviceId);
            setupEntrantView(entrant);
        }).addOnFailureListener(e -> handleFailure("Error identifying user profile."));
    }

    /**
     * Sets up the view showing events the user has registered for.
     *
     * @param entrant The current entrant.
     */
    private void setupEntrantView(Entrant entrant) {
        EventAdapter adapter = new EventAdapter();
        recyclerView.setAdapter(adapter);

        entrant.getRegisteredEvents().addOnSuccessListener(events -> {
            Log.d("HistoryTab", "Loaded " + (events != null ? events.size() : 0) + " signed up events");
            updateUI(events, "You haven't joined any events yet.");
            adapter.setEvents(events);
        }).addOnFailureListener(e -> handleFailure("Query failed. You might need to create a Firestore Index. Check Logcat."));
    }

    /**
     * Updates the UI based on the list of events fetched.
     *
     * @param events       The list of events.
     * @param emptyMessage The message to show if the list is empty.
     */
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

    /**
     * Handles failures in data fetching by logging the error and showing a message to the user.
     *
     * @param message The error message.
     */
    private void handleFailure(String message) {
        Log.e("HistoryTab", message);
        if (loadingBar != null) loadingBar.setVisibility(View.GONE);
        if (emptyText != null) {
            emptyText.setText(message);
            emptyText.setVisibility(View.VISIBLE);
        }
    }
}
