package com.example.thevms.ui.Event;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.Comment;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Notification;
import com.example.thevms.model.Organizer;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for organizers to manage their events.
 * Displays event details, attendee lists with status filtering, and controls for running a lottery or cancelling events.
 */
public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder> {

    // Display labels shown in the dropdown — matching the required terminology
    private static final String[] STATUS_LABELS = {
            "Waiting", "Selected", "Accepted", "Not Selected", "Cancelled", "Declined"
    };

    private static final String[] STATUS_VALUES = {
            "waiting", "selected", "accepted", "rejected", "cancelled", "declined"
    };

    private List<Event> events = new ArrayList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private OnEventCancelListener cancelListener;
    private OnEventUpdatePosterListener updatePosterListener;

    /**
     * Interface for listening to event cancellation actions.
     */
    public interface OnEventCancelListener {
        /**
         * Called when an event is cancelled by the organizer.
         *
         * @param event The event being cancelled.
         */
        void onCancel(Event event);
    }

    /**
     * Interface for listening to update poster actions.
     */
    public interface OnEventUpdatePosterListener {
        /**
         * Called when the organizer wants to update the event poster.
         *
         * @param event The event whose poster is being updated.
         */
        void onUpdatePoster(Event event);
    }

    /**
     * Updates the list of events to be displayed.
     *
     * @param events The new list of events.
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Sets the listener for event cancellation actions.
     *
     * @param cancelListener The listener to set.
     */
    public void setOnEventCancelListener(OnEventCancelListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    /**
     * Sets the listener for update poster actions.
     *
     * @param updatePosterListener The listener to set.
     */
    public void setOnEventUpdatePosterListener(OnEventUpdatePosterListener updatePosterListener) {
        this.updatePosterListener = updatePosterListener;
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
        holder.bind(event, dateFormat, cancelListener, updatePosterListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class for organizer event cards.
     * Manages nested RecyclerView for attendees and event management controls.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, distanceText, waitlistText, dateText, descriptionText, exportCsvText;
        Button cancelBtn, lotteryBtn, postCommentBtn, updatePosterBtn, inviteBtn;
        RecyclerView attendeesRv, commentsRv;
        ImageView posterImage;
        Spinner statusSpinner;
        EditText commentEditText;
        AttendeeAdapter attendeeAdapter;
        CommentAdapter commentAdapter;
        DatabaseHandler dbHandler;
        FirebaseFirestore db;
        ListenerRegistration commentsListener;

        // Stored so the CSV export can use it for the filename
        String currentEventName = "";

        /**
         * Initializes the ViewHolder and sets up nested UI components like the attendee list and status spinner.
         *
         * @param itemView The view representing a single organizer event card.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_event_name);
            distanceText = itemView.findViewById(R.id.tv_distance);
            waitlistText = itemView.findViewById(R.id.tv_waitlist_count);
            dateText = itemView.findViewById(R.id.tv_event_date);
            descriptionText = itemView.findViewById(R.id.tv_description);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_event);
            lotteryBtn = itemView.findViewById(R.id.btn_run_lottery);
            updatePosterBtn = itemView.findViewById(R.id.btn_update_poster);
            inviteBtn = itemView.findViewById(R.id.btn_invite_entrants);
            attendeesRv = itemView.findViewById(R.id.rv_attendees);
            commentsRv = itemView.findViewById(R.id.rv_comments);
            posterImage = itemView.findViewById(R.id.iv_event_poster);
            statusSpinner = itemView.findViewById(R.id.spinner_attendee_status);
            exportCsvText = itemView.findViewById(R.id.tv_export_csv);
            commentEditText = itemView.findViewById(R.id.et_organizer_comment);
            postCommentBtn = itemView.findViewById(R.id.btn_post_organizer_comment);

            attendeesRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            attendeeAdapter = new AttendeeAdapter();
            attendeesRv.setAdapter(attendeeAdapter);

            commentsRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            commentAdapter = new CommentAdapter();
            commentAdapter.setShowDeleteButton(true);
            commentsRv.setAdapter(commentAdapter);

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
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Wire export CSV button — exports whatever is currently filtered in the list
            exportCsvText.setOnClickListener(v ->
                    attendeeAdapter.exportFilteredListAsCsv(
                            itemView.getContext(),
                            currentEventName
                    )
            );
        }

        /**
         * Executes the lottery for an event, randomly selecting winners from the waiting list.
         *
         * @param event The event for which to run the lottery.
         */
        private void runLottery(Event event, int requestedWinners) {
            dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId()))
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<DocumentSnapshot> waitingList = new ArrayList<>();
                        int currentSelectedOrAccepted = 0;

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String status = doc.getString("status");
                            if (DatabaseHandler.STATUS_WAITING.equals(status)) {
                                waitingList.add(doc);
                            } else if (DatabaseHandler.STATUS_SELECTED.equals(status) || 
                                       DatabaseHandler.STATUS_ACCEPTED.equals(status)) {
                                currentSelectedOrAccepted++;
                            }
                        }

                        if (waitingList.isEmpty()) {
                            Toast.makeText(itemView.getContext(), "No one in waiting list", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int winnersCount = determineWinnerCount(
                                requestedWinners,
                                waitingList.size(),
                                event.getMaxAttendees(),
                                currentSelectedOrAccepted);

                        if (winnersCount <= 0) {
                            Toast.makeText(itemView.getContext(), "Not enough spots available for new winners", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Collections.shuffle(waitingList);

                        for (int i = 0; i < winnersCount; i++) {
                            DocumentSnapshot doc = waitingList.get(i);
                            Map<String, Object> data = new HashMap<>();
                            data.put("status", DatabaseHandler.STATUS_SELECTED);
                            dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), doc.getId(), data);
                        }

                        Toast.makeText(itemView.getContext(), "Selected " + winnersCount + " entrant(s)!", Toast.LENGTH_SHORT).show();
                        bind(event, dateFormat, null, null); // Refresh list
                    });
        }

        private void showWinnerSelectionDialog(Event event) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_pick_winners, null);
            TextInputEditText input = dialogView.findViewById(R.id.et_winner_count);

            builder.setTitle("Select winners")
                    .setView(dialogView)
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Select", null);

            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(d -> {
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setOnClickListener(v -> {
                    String raw = input.getText() != null ? input.getText().toString().trim() : "";
                    if (raw.isEmpty()) {
                        input.setError("Required");
                        return;
                    }
                    int requested;
                    try {
                        requested = Integer.parseInt(raw);
                    } catch (NumberFormatException ex) {
                        input.setError("Enter a whole number");
                        return;
                    }
                    if (requested <= 0) {
                        input.setError("Must be greater than 0");
                        return;
                    }
                    dialog.dismiss();
                    runLottery(event, requested);
                });
            });
            dialog.show();
        }

        /**
         * Binds an event's data to the ViewHolder and fetches the list of entrants.
         *
         * @param event                The event to bind.
         * @param dateFormat           The date format for the event date.
         * @param cancelListener       The listener for event cancellation.
         * @param updatePosterListener The listener for poster update.
         */
        public void bind(Event event, SimpleDateFormat dateFormat, OnEventCancelListener cancelListener, OnEventUpdatePosterListener updatePosterListener) {
            String eventId = String.valueOf(event.getEventId());
            itemView.setTag(eventId);

            currentEventName = event.getName() != null ? event.getName() : "Event";

            Organizer organizer = event.getOrganizer();
            String organizerDisplayName = buildFullName(
                    organizer != null ? organizer.getFirstName() : null,
                    organizer != null ? organizer.getLastName() : null,
                    "Organizer");
            String resolvedOrganizerId = organizer != null ? organizer.getDeviceId() : null;
            if (resolvedOrganizerId == null) {
                resolvedOrganizerId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            attendeeAdapter.setEventContext(eventId, currentEventName, resolvedOrganizerId, organizerDisplayName);

            nameText.setText(event.getName());
            descriptionText.setText(event.getDescription());

            if (event.getEventStartTime() != null) {
                dateText.setText("🗓 " + dateFormat.format(event.getEventStartTime()));
            }

            if (event.getPhoto() != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(event.getPhoto(), 0, event.getPhoto().length);
                posterImage.setImageBitmap(bitmap);
                posterImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                posterImage.setAlpha(1.0f);
                posterImage.clearColorFilter();
            } else {
                posterImage.setImageResource(R.drawable.ic_launcher_foreground);
                posterImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                posterImage.setAlpha(0.3f);
            }

            event.fetchEntrantCount().addOnSuccessListener(count ->
                    waitlistText.setText(count + " people in waitlist")
            );

            cancelBtn.setOnClickListener(v -> {
                if (cancelListener != null) cancelListener.onCancel(event);
            });

            lotteryBtn.setOnClickListener(v -> showWinnerSelectionDialog(event));

            updatePosterBtn.setOnClickListener(v -> {
                if (updatePosterListener != null) updatePosterListener.onUpdatePoster(event);
            });

            inviteBtn.setVisibility(event.isPrivate() ? View.VISIBLE : View.GONE);
            inviteBtn.setOnClickListener(v -> showInviteDialog(event));

            postCommentBtn.setOnClickListener(v -> postOrganizerComment(eventId));

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

            setupComments(eventId);
        }

        @VisibleForTesting
        public static int determineWinnerCount(int requested, int waitingCount, Integer maxAttendees, int currentSelected) {
            if (requested <= 0 || waitingCount <= 0) {
                return 0;
            }

            int capped = Math.min(requested, waitingCount);
            if (maxAttendees != null && maxAttendees > 0) {
                int spotsLeft = maxAttendees - currentSelected;
                if (spotsLeft <= 0) {
                    return 0;
                }
                capped = Math.min(capped, spotsLeft);
            }
            return capped;
        }

        private void showInviteDialog(Event event) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_invite_entrants, null);
            builder.setView(dialogView);

            EditText searchEt = dialogView.findViewById(R.id.et_search_entrants);
            RecyclerView resultsRv = dialogView.findViewById(R.id.rv_search_results);
            Button closeBtn = dialogView.findViewById(R.id.btn_close_invite);

            InviteEntrantAdapter inviteAdapter = new InviteEntrantAdapter();
            resultsRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            resultsRv.setAdapter(inviteAdapter);

            AlertDialog dialog = builder.create();

            List<Entrant> allUsers = new ArrayList<>();
            dbHandler.getAllUsers().addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Entrant u = Entrant.fromMap(doc.getId(), doc.getData());
                    if (u != null) allUsers.add(u);
                }
            });

            searchEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().toLowerCase();
                    if (query.isEmpty()) {
                        inviteAdapter.setEntrants(new ArrayList<>());
                        return;
                    }
                    List<Entrant> filtered = new ArrayList<>();
                    for (Entrant u : allUsers) {
                        boolean matchesName = (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(query))
                                || (u.getLastName() != null && u.getLastName().toLowerCase().contains(query));
                        boolean matchesEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(query);
                        boolean matchesPhone = u.getPhoneNumber() != null && u.getPhoneNumber().contains(query);

                        if (matchesName || matchesEmail || matchesPhone) {
                            filtered.add(u);
                        }
                    }
                    inviteAdapter.setEntrants(filtered);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            inviteAdapter.setOnInviteClickListener(entrant -> {
                // Invite logic
                Map<String, Object> registrationData = new HashMap<>();
                registrationData.put("entrantId", entrant.getDeviceId());
                registrationData.put("status", DatabaseHandler.STATUS_WAITING);
                registrationData.put("registrationTime", new Date());

                dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), entrant.getDeviceId(), registrationData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "Invited " + entrant.getFirstName(), Toast.LENGTH_SHORT).show();
                            // Trigger notification
                            String organizerId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                            Notification.createWaitingListInvite(organizerId, "Organizer", entrant.getDeviceId(), String.valueOf(event.getEventId()), String.valueOf(event.getEventId()), event.getName()).send();
                        });
            });

            closeBtn.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        private void setupComments(String eventId) {
            if (commentsListener != null) {
                commentsListener.remove();
            }

            commentsListener = dbHandler.listenToComments(eventId, (value, error) -> {
                if (error != null) {
                    Log.w("OrganizerEventAdapter", "Listen failed.");
                    return;
                }

                if (value == null) return;

                List<Comment> comments = new ArrayList<>();
                List<String> commentIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    comments.add(doc.toObject(Comment.class));
                    commentIds.add(doc.getId());
                }
                commentAdapter.setComments(comments, commentIds);
            });

            commentAdapter.setOnCommentDeleteListener((comment, commentId) -> {
                showDeleteCommentConfirmation(eventId, commentId);
            });
        }

        private void postOrganizerComment(String eventId) {
            String text = commentEditText.getText().toString().trim();
            if (text.isEmpty()) return;

            String deviceId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            dbHandler.getUser(deviceId).addOnSuccessListener(userDoc -> {
                String firstName = userDoc.getString("firstName");
                String lastName = userDoc.getString("lastName");
                if (firstName == null) firstName = "Organizer";
                if (lastName == null) lastName = "";

                Comment comment = new Comment(deviceId, firstName, lastName, text, new Date(), true);
                dbHandler.addComment(eventId, comment).addOnSuccessListener(aVoid -> {
                    commentEditText.setText("");
                    Toast.makeText(itemView.getContext(), "Comment posted", Toast.LENGTH_SHORT).show();
                });
            });
        }

        private void showDeleteCommentConfirmation(String eventId, String commentId) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_cancel_confirmation, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            TextView title = dialogView.findViewById(R.id.tv_dialog_title);
            TextView message = dialogView.findViewById(R.id.tv_dialog_message);
            Button btnBack = dialogView.findViewById(R.id.btn_dialog_back);
            Button btnYes = dialogView.findViewById(R.id.btn_dialog_yes);
            ImageView ivClose = dialogView.findViewById(R.id.iv_close);

            if (title != null) title.setText(R.string.delete_comment_title);
            if (message != null) message.setText(R.string.delete_comment_message);
            if (btnYes != null) btnYes.setText(R.string.delete_comment_confirm);
            if (btnBack != null) btnBack.setText(R.string.delete_comment_cancel);

            if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
            if (ivClose != null) ivClose.setOnClickListener(v -> dialog.dismiss());
            if (btnYes != null) {
                btnYes.setOnClickListener(v -> {
                    dbHandler.deleteComment(eventId, commentId).addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), R.string.comment_deleted_toast, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            }

            dialog.show();
        }

        private String buildFullName(String first, String last, String fallback) {
            StringBuilder builder = new StringBuilder();
            if (first != null && !first.isEmpty()) {
                builder.append(first);
            }
            if (last != null && !last.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(last);
            }
            if (builder.length() == 0) {
                return fallback;
            }
            return builder.toString();
        }

        /**
         * Cancels a specific entrant's selection and automatically invites the next person from the waiting list.
         *
         * @param eventId            The ID of the event.
         * @param cancelledEntrantId The ID of the entrant to cancel.
         */
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
