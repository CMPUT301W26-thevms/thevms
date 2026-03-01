package com.example.thevms.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.thevms.R;
import com.example.thevms.model.Event;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Helper class to inflate and populate event cards.
 */
public class EventCardHelper {

    /**
     * Creates and populates an event card view from an Event object.
     *
     * @param parent The parent view group.
     * @param event  The event data.
     * @return A populated event card view.
     */
    public static View createEventCard(ViewGroup parent, Event event) {
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);

        TextView nameView = card.findViewById(R.id.event_name);
        TextView statusView = card.findViewById(R.id.event_status_info);
        TextView timeView = card.findViewById(R.id.event_time_info);
        TextView locationView = card.findViewById(R.id.event_location_info);

        if (nameView != null) {
            nameView.setText(event.getName());
        }

        if (statusView != null) {
            int count = (event.getEntrantList() != null) ? event.getEntrantList().size() : 0;
            statusView.setText("☆ " + count + " people joined");
        }

        if (timeView != null) {
            if (event.getEventStartTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                timeView.setText("Starts " + sdf.format(event.getEventStartTime()));
            } else {
                timeView.setText("Time TBD");
            }
        }

        if (locationView != null) {
            locationView.setText("📍 Nearby");
        }

        return card;
    }
}
