package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder> {

    private List<Event> events = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private OnEventCancelListener cancelListener;

    public interface OnEventCancelListener {
        void onCancel(Event event);
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setOnEventCancelListener(OnEventCancelListener listener) {
        this.cancelListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.organizer_event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, dateFormat, cancelListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, distanceText, waitlistText, dateText, descriptionText;
        Button cancelBtn;
        RecyclerView attendeesRv;
        AttendeeAdapter attendeeAdapter;
        DatabaseHandler dbHandler;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_event_name);
            distanceText = itemView.findViewById(R.id.tv_distance);
            waitlistText = itemView.findViewById(R.id.tv_waitlist_count);
            dateText = itemView.findViewById(R.id.tv_event_date);
            descriptionText = itemView.findViewById(R.id.tv_description);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_event);
            attendeesRv = itemView.findViewById(R.id.rv_attendees);

            attendeesRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            attendeeAdapter = new AttendeeAdapter();
            attendeesRv.setAdapter(attendeeAdapter);
            dbHandler = new DatabaseHandler();
        }

        public void bind(Event event, SimpleDateFormat dateFormat, OnEventCancelListener cancelListener) {
            nameText.setText(event.getName());
            descriptionText.setText(event.getDescription());
            
            if (event.getEventStartTime() != null) {
                dateText.setText("🗓 " + dateFormat.format(event.getEventStartTime()));
            }

            // Fetch and display entrant count
            event.fetchEntrantCount().addOnSuccessListener(count -> {
                waitlistText.setText(count + " people in waitlist");
            });

            cancelBtn.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancel(event);
                }
            });

            // Fetch entrants and their profile details to display names
            dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId())).addOnSuccessListener(queryDocumentSnapshots -> {
                List<Entrant> entrants = new ArrayList<>();
                List<com.google.android.gms.tasks.Task<DocumentSnapshot>> profileTasks = new ArrayList<>();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String entrantId = doc.getString("entrantId");
                    if (entrantId != null) {
                        profileTasks.add(dbHandler.getUser(entrantId));
                    }
                }

                if (!profileTasks.isEmpty()) {
                    com.google.android.gms.tasks.Tasks.whenAllSuccess(profileTasks).addOnSuccessListener(profiles -> {
                        for (Object obj : profiles) {
                            DocumentSnapshot profileDoc = (DocumentSnapshot) obj;
                            if (profileDoc.exists()) {
                                Map<String, Object> data = profileDoc.getData();
                                if (data != null) {
                                    Entrant entrant = Entrant.fromMap(profileDoc.getId(), data);
                                    entrants.add(entrant);
                                }
                            }
                        }
                        attendeeAdapter.setAttendees(entrants);
                    });
                } else {
                    attendeeAdapter.setAttendees(new ArrayList<>());
                }
            });
        }
    }
}
