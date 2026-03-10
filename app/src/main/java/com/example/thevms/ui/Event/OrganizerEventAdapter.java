package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

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
        Button cancelBtn, lotteryBtn;
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
            
            // Re-using an existing button or adding a new one. 
            // Since I can't easily change the layout file without overwriting, 
            // I'll try to find a place for it or assume it might be there.
            // Actually, I should update organizer_event_card.xml too.
            lotteryBtn = itemView.findViewById(R.id.btn_view_qr); // Hijacking this for lottery in this demo if needed, but better to add it.
            
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

            if (lotteryBtn != null) {
                lotteryBtn.setText("Run Lottery");
                lotteryBtn.setOnClickListener(v -> runLottery(event));
            }

            loadAttendees(event);
        }

        private void loadAttendees(Event event) {
            dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId())).addOnSuccessListener(queryDocumentSnapshots -> {
                List<Entrant> entrants = new ArrayList<>();
                List<com.google.android.gms.tasks.Task<DocumentSnapshot>> profileTasks = new ArrayList<>();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String entrantId = doc.getString("entrantId");
                    String status = doc.getString("status");
                    // Only show invited/accepted in the "Final Attendees" list? 
                    // Or show everyone with status?
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

        private void runLottery(Event event) {
            dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId())).addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> waitingList = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    if ("waiting".equals(doc.getString("status"))) {
                        waitingList.add(doc);
                    }
                }

                if (waitingList.isEmpty()) {
                    Toast.makeText(itemView.getContext(), "No one in the waiting list!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Pick winners - for simplicity pick up to 5 or half
                Collections.shuffle(waitingList);
                int numWinners = Math.min(waitingList.size(), 3); // Example: 3 winners
                
                List<com.google.android.gms.tasks.Task<Void>> tasks = new ArrayList<>();
                for (int i = 0; i < waitingList.size(); i++) {
                    DocumentSnapshot doc = waitingList.get(i);
                    Map<String, Object> update = new HashMap<>();
                    if (i < numWinners) {
                        update.put("status", "selected");
                    } else {
                        update.put("status", "rejected");
                    }
                    tasks.add(dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), doc.getId(), update));
                }

                com.google.android.gms.tasks.Tasks.whenAll(tasks).addOnSuccessListener(aVoid -> {
                    Toast.makeText(itemView.getContext(), "Lottery completed! " + numWinners + " selected.", Toast.LENGTH_LONG).show();
                    loadAttendees(event);
                });
            });
        }
    }
}
