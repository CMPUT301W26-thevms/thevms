package com.example.thevms.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thevms.R;
import com.example.thevms.model.Event;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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
        holder.nameTextView.setText(event.getName());
        if (event.getEventStartTime() != null) {
            holder.dateTextView.setText("Starts: " + dateFormat.format(event.getEventStartTime()));
        } else {
            holder.dateTextView.setText("Date TBD");
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView dateTextView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Based on event_card.xml, I need to find the correct IDs.
            // Looking at event_card.xml again... it doesn't have IDs for the text views!
            // I should update event_card.xml to have IDs.
            nameTextView = itemView.findViewById(R.id.event_name_text);
            dateTextView = itemView.findViewById(R.id.event_date_text);
        }
    }
}
