package com.example.thevms.ui.Event;

import android.annotation.SuppressLint;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, dateFormat);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView statusTextView;
        TextView timeTextView;
        TextView locationTextView;
        Button joinButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.event_name);
            statusTextView = itemView.findViewById(R.id.event_status_info);
            timeTextView = itemView.findViewById(R.id.event_time_info);
            locationTextView = itemView.findViewById(R.id.event_location_info);
            joinButton = itemView.findViewById(R.id.btn_join_event);
        }

        public void bind(Event event, SimpleDateFormat dateFormat) {
            nameTextView.setText(event.getName());

            // Asynchronously fetch the entrant count from the sub-collection
            updateEntrantCount(event);

            if (event.getEventStartTime() != null) {
                timeTextView.setText(String.format("Starts %s", dateFormat.format(event.getEventStartTime())));
            } else {
                timeTextView.setText("Time TBD");
            }

            locationTextView.setText("📍 Nearby");

            joinButton.setOnClickListener(v -> {
                @SuppressLint("HardwareIds")
                String deviceId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

                Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                    event.addEntrant(entrant).addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), "Successfully joined " + event.getName(), Toast.LENGTH_SHORT).show();
                        // Update UI to show new count
                        updateEntrantCount(event);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "Failed to join event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(itemView.getContext(), "Error retrieving user profile", Toast.LENGTH_SHORT).show();
                });
            });
        }

        private void updateEntrantCount(Event event) {
            event.fetchEntrantCount().addOnSuccessListener(count -> {
                statusTextView.setText(String.format(Locale.getDefault(), "☆ %d people joined", count));
            }).addOnFailureListener(e -> {
                statusTextView.setText("☆ -- people joined");
            });
        }
    }
}
