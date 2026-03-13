package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder> {

    // Display labels shown in the dropdown — must match Firestore status values exactly
    private static final String[] STATUS_LABELS = {
            "Waiting", "Selected", "Accepted", "Rejected", "Cancelled", "Declined"
    };

    private static final String[] STATUS_VALUES = {
            "waiting", "selected", "accepted", "rejected", "cancelled", "declined"
    };

    private List<Event> events = new ArrayList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private OnEventCancelListener cancelListener;

    public interface OnEventCancelListener {
        void onCancel(Event event);
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setOnEventCancelListener(OnEventCancelListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_event_card, parent, false);
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
        TextView nameText, distanceText, waitlistText, dateText, descriptionText, exportCsvText;
        Button cancelBtn, lotteryBtn;
        RecyclerView attendeesRv;
        Spinner statusSpinner;
        AttendeeAdapter attendeeAdapter;
        DatabaseHandler dbHandler;
        FirebaseFirestore db;

        // Stored so the CSV export can use it for the filename
        String currentEventName = "";

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_event_name);
            distanceText = itemView.findViewById(R.id.tv_distance);
            waitlistText = itemView.findViewById(R.id.tv_waitlist_count);
            dateText = itemView.findViewById(R.id.tv_event_date);
            descriptionText = itemView.findViewById(R.id.tv_description);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_event);
            lotteryBtn = itemView.findViewById(R.id.btn_run_lottery);
            attendeesRv = itemView.findViewById(R.id.rv_attendees);
            statusSpinner = itemView.findViewById(R.id.spinner_attendee_status);
            exportCsvText = itemView.findViewById(R.id.tv_export_csv);

            attendeesRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            attendeeAdapter = new AttendeeAdapter();
            attendeesRv.setAdapter(attendeeAdapter);
            dbHandler = new DatabaseHandler();
            db = FirebaseFirestore.getInstance();

            // Wire cancel listener — cancels entrant and promotes next waitlisted randomly
            attendeeAdapter.setOnCancelEntrantListener(item -> {
                if (!item.isCancellable()) return;
                String eventId = (String) itemView.getTag();
                cancelEntrantAndSelectNext(eventId, item.getEntrant().getDeviceId());
            });

            // Set up spinner with status labels
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    STATUS_LABELS
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            statusSpinner.setAdapter(spinnerAdapter);

            // Filter attendee list when organizer picks a status
            statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    attendeeAdapter.filterByStatus(STATUS_VALUES[position]);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Wire export CSV button — exports whatever is currently filtered in the list
            exportCsvText.setOnClickListener(v ->
                    attendeeAdapter.exportFilteredListAsCsv(
                            itemView.getContext(),
                            currentEventName
                    )
            );
        }

        private void runLottery(Event event) {
            dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId()))
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<DocumentSnapshot> waitingList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (DatabaseHandler.STATUS_WAITING.equals(doc.getString("status"))) {
                                waitingList.add(doc);
                            }
                        }

                        if (waitingList.isEmpty()) {
                            Toast.makeText(itemView.getContext(), "No one in waiting list", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        java.util.Collections.shuffle(waitingList);

                        // Use maxAttendees if specified, otherwise pick a default (e.g., 3)
                        int capacity = (event.getMaxAttendees() != null && event.getMaxAttendees() > 0) ? event.getMaxAttendees() : 3;
                        int winnersCount = Math.min(waitingList.size(), capacity);

                        for (int i = 0; i < winnersCount; i++) {
                            DocumentSnapshot doc = waitingList.get(i);
                            Map<String, Object> data = new HashMap<>();
                            data.put("status", DatabaseHandler.STATUS_SELECTED);
                            dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), doc.getId(), data);
                        }

                        Toast.makeText(itemView.getContext(), "Selected " + winnersCount + " entrants!", Toast.LENGTH_SHORT).show();
                        bind(event, dateFormat, null); // Refresh list
                    });
        }

        public void bind(Event event, SimpleDateFormat dateFormat, OnEventCancelListener cancelListener) {
            String eventId = String.valueOf(event.getEventId());
            itemView.setTag(eventId);

            currentEventName = event.getName() != null ? event.getName() : "Event";

            nameText.setText(event.getName());
            descriptionText.setText(event.getDescription());

            if (event.getEventStartTime() != null) {
                dateText.setText("🗓 " + dateFormat.format(event.getEventStartTime()));
            }

            event.fetchEntrantCount().addOnSuccessListener(count ->
                    waitlistText.setText(count + " people in waitlist")
            );

            cancelBtn.setOnClickListener(v -> {
                if (cancelListener != null) cancelListener.onCancel(event);
            });

            lotteryBtn.setOnClickListener(v -> runLottery(event));

            // Reset spinner to "Waiting" each time a card is bound
            statusSpinner.setSelection(0);

            // Step 1: fetch all entrant docs from events/{eventId}/entrants/
            dbHandler.getEntrantsForEvent(eventId).addOnSuccessListener(queryDocumentSnapshots -> {

                // Map entrantId → status (scoped to this event)
                Map<String, String> statusMap = new HashMap<>();
                List<com.google.android.gms.tasks.Task<DocumentSnapshot>> profileTasks = new ArrayList<>();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String entrantId = doc.getString("entrantId");
                    String status = doc.getString("status");
                    if (entrantId != null) {
                        statusMap.put(entrantId, status);
                        profileTasks.add(dbHandler.getUser(entrantId));
                    }
                }

                if (!profileTasks.isEmpty()) {
                    // Step 2: fetch user profiles in parallel
                    com.google.android.gms.tasks.Tasks.whenAllSuccess(profileTasks)
                            .addOnSuccessListener(profiles -> {
                                List<AttendeeItem> attendeeItems = new ArrayList<>();
                                for (Object obj : profiles) {
                                    DocumentSnapshot profileDoc = (DocumentSnapshot) obj;
                                    if (profileDoc.exists()) {
                                        Map<String, Object> data = profileDoc.getData();
                                        if (data != null) {
                                            Entrant entrant = Entrant.fromMap(profileDoc.getId(), data);
                                            String status = statusMap.get(profileDoc.getId());
                                            attendeeItems.add(new AttendeeItem(entrant, status));
                                        }
                                    }
                                }
                                attendeeAdapter.setAttendees(attendeeItems);
                            });
                } else {
                    attendeeAdapter.setAttendees(new ArrayList<>());
                }
            });
        }

        private void cancelEntrantAndSelectNext(String eventId, String cancelledEntrantId) {
            Map<String, Object> cancelData = new HashMap<>();
            cancelData.put("status", DatabaseHandler.STATUS_CANCELLED);
            dbHandler.updateEntrantStatus(eventId, cancelledEntrantId, cancelData)
                    .addOnSuccessListener(unused -> {
                        dbHandler.selectAndInviteNextEntrant(eventId);
                    });
        }
    }
}
