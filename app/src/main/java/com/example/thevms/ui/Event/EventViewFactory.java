package com.example.thevms.ui.Event;

import android.annotation.SuppressLint;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Factory class responsible for inflating and binding data to event card views.
 */
public class EventViewFactory {

    /**
     * Inflates an event card layout and populates it with data from the provided Event object.
     *
     * @param parent The parent view group used for layout inflation context.
     * @param event  The event model containing the data to display.
     * @return A View instance representing the populated event card.
     */
    public static View createEventCard(ViewGroup parent, Event event) {
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);

        TextView nameView = card.findViewById(R.id.event_name);
        TextView statusView = card.findViewById(R.id.event_status_info);
        TextView timeView = card.findViewById(R.id.event_time_info);
        TextView locationView = card.findViewById(R.id.event_location_info);
        Button joinButton = card.findViewById(R.id.btn_join_event);

        if (nameView != null) {
            nameView.setText(event.getName());
        }

        if (statusView != null) {
            int count = (event.getEntrantList() != null) ? event.getEntrantList().size() : 0;
            statusView.setText(String.format(Locale.getDefault(), "☆ %d people joined", count));
        }

        if (timeView != null) {
            if (event.getEventStartTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                timeView.setText(String.format("Starts %s", sdf.format(event.getEventStartTime())));
            } else {
                timeView.setText("Time TBD");
            }
        }

        if (locationView != null) {
            // TODO: Replace with event.getLocation() if available in your model
            locationView.setText("📍 Nearby");
        }

        if (joinButton != null) {
            joinButton.setOnClickListener(v -> {
                @SuppressLint("HardwareIds")
                String deviceId = Settings.Secure.getString(parent.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                
                Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                    event.addEntrant(entrant).addOnSuccessListener(aVoid -> {
                        Toast.makeText(parent.getContext(), "Successfully joined " + event.getName(), Toast.LENGTH_SHORT).show();
                        // Update UI to show new count if needed
                        if (statusView != null) {
                            int count = (event.getEntrantList() != null) ? event.getEntrantList().size() : 0;
                            statusView.setText(String.format(Locale.getDefault(), "☆ %d people joined", count));
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(parent.getContext(), "Failed to join event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(parent.getContext(), "Error retrieving user profile", Toast.LENGTH_SHORT).show();
                });
            });
        }

        return card;
    }
}
