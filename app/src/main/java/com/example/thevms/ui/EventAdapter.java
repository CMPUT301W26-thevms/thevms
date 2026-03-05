package com.example.thevms.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.thevms.R;
import com.example.thevms.model.Event;
import com.example.thevms.ui.Event.EventViewFactory;
import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private boolean isAdmin = false;

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
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
        // Use the factory to bind data and provide a callback for deletion
        EventViewFactory.bindEventCard(holder.itemView, event, isAdmin, () -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                events.remove(currentPos);
                notifyItemRemoved(currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
