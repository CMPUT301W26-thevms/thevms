package com.example.thevms.ui.Event;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private boolean isAdmin = false;
    private boolean managementMode = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final Set<Long> expandedEventIds = new HashSet<>();

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
        notifyDataSetChanged();
    }

    public void setManagementMode(boolean managementMode) {
        this.managementMode = managementMode;
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
        boolean isExpanded = expandedEventIds.contains(event.getEventId());

        holder.bind(event, dateFormat, fullDateFormat, isAdmin, managementMode, isExpanded,
                () -> {
                    int currentPos = holder.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        events.remove(currentPos);
                        notifyItemRemoved(currentPos);
                    }
                },
                (expand) -> {
                    if (expand) {
                        expandedEventIds.add(event.getEventId());
                    } else {
                        expandedEventIds.remove(event.getEventId());
                    }
                    notifyItemChanged(holder.getAdapterPosition());
                }
        );
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, statusTextView, timeTextView, locationTextView;
        Button joinButton, removeButton, leaveButton, acceptButton, declineButton, expandButton;
        ImageView eventImageView;
        TextView regStatusMessage, descriptionTextView, regStartTextView, regEndTextView, eventStartTextView, eventEndTextView;
        View expandableDetails;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.event_name);
            statusTextView = itemView.findViewById(R.id.event_status_info);
            timeTextView = itemView.findViewById(R.id.event_time_info);
            locationTextView = itemView.findViewById(R.id.event_location_info);
            joinButton = itemView.findViewById(R.id.btn_join_event);
            removeButton = itemView.findViewById(R.id.btn_remove_event);
            leaveButton = itemView.findViewById(R.id.btn_leave_event);
            acceptButton = itemView.findViewById(R.id.btn_accept_event);
            declineButton = itemView.findViewById(R.id.btn_decline_event);
            eventImageView = itemView.findViewById(R.id.event_image);
            regStatusMessage = itemView.findViewById(R.id.event_registration_status_message);
            expandableDetails = itemView.findViewById(R.id.expandable_details);
            descriptionTextView = itemView.findViewById(R.id.event_description);
            regStartTextView = itemView.findViewById(R.id.event_reg_start_details);
            regEndTextView = itemView.findViewById(R.id.event_reg_end_details);
            eventStartTextView = itemView.findViewById(R.id.event_start_details);
            eventEndTextView = itemView.findViewById(R.id.event_end_details);
            expandButton = itemView.findViewById(R.id.btn_expand_details);
        }

        public void bind(Event event, SimpleDateFormat dateFormat, SimpleDateFormat fullDateFormat,
                         boolean isAdmin, boolean managementMode, boolean isExpanded, Runnable onDelete,
                         java.util.function.Consumer<Boolean> onToggleExpand) {

            if (nameTextView != null) nameTextView.setText(event.getName());
            if (timeTextView != null && event.getEventStartTime() != null) {
                timeTextView.setText(String.format("Starts %s", dateFormat.format(event.getEventStartTime())));
            }
            if (locationTextView != null) {
                locationTextView.setText("📍 " + (event.getLocationName() != null ? event.getLocationName() : "No location"));
            }

            @SuppressLint("HardwareIds")
            String deviceId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            DatabaseHandler dbHandler = new DatabaseHandler();

            if (isAdmin) {
                if (removeButton != null) {
                    removeButton.setVisibility(View.VISIBLE);
                    removeButton.setOnClickListener(v -> showDeleteConfirmation(event, onDelete));
                }
            } else {
                if (removeButton != null) removeButton.setVisibility(View.GONE);
            }

            updateEntrantCount(event, null);
            dbHandler.getEntrantStatus(String.valueOf(event.getEventId()), deviceId).addOnSuccessListener(status -> {
                updateUIBasedOnStatus(status, event, deviceId, dbHandler);
            });

            // Bind detailed info
            if (descriptionTextView != null) {
                descriptionTextView.setText(event.getDescription() != null ? event.getDescription() : "No description provided.");
            }
            if (regStartTextView != null) {
                regStartTextView.setText("Registration Starts: " + (event.getRegistrationStartTime() != null ? fullDateFormat.format(event.getRegistrationStartTime()) : "TBD"));
            }
            if (regEndTextView != null) {
                regEndTextView.setText("Registration Ends: " + (event.getRegistrationEndTime() != null ? fullDateFormat.format(event.getRegistrationEndTime()) : "TBD"));
            }
            if (eventStartTextView != null) {
                eventStartTextView.setText("Event Starts: " + (event.getEventStartTime() != null ? fullDateFormat.format(event.getEventStartTime()) : "TBD"));
            }
            if (eventEndTextView != null) {
                eventEndTextView.setText("Event Ends: " + (event.getEventEndTime() != null ? fullDateFormat.format(event.getEventEndTime()) : "TBD"));
            }

            // Handle Expansion state
            if (expandableDetails != null) {
                expandableDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            }
            if (expandButton != null) {
                expandButton.setText(isExpanded ? "Show Less" : "Show More Details");
                expandButton.setOnClickListener(v -> onToggleExpand.accept(!isExpanded));
            }
        }

        private void updateEntrantCount(Event event, String statusPrefix) {
            if (statusTextView == null) return;
            event.fetchEntrantCount().addOnSuccessListener(count -> {
                String text = String.format(Locale.getDefault(), "☆ %d people joined", count);
                if (statusPrefix != null && !statusPrefix.isEmpty()) {
                    text = statusPrefix + " (" + text + ")";
                }
                statusTextView.setText(text);
            });
        }

        private void updateUIBasedOnStatus(String status, Event event, String deviceId, DatabaseHandler dbHandler) {
            joinButton.setVisibility(View.GONE);
            leaveButton.setVisibility(View.GONE);
            acceptButton.setVisibility(View.GONE);
            declineButton.setVisibility(View.GONE);

            String statusPrefix = "Not joined";

            if (status == null) {
                joinButton.setVisibility(View.VISIBLE);
                setupJoinButton(event, deviceId);
            } else {
                switch (status) {
                    case DatabaseHandler.STATUS_WAITING:
                        leaveButton.setVisibility(View.VISIBLE);
                        statusPrefix = "Status: Waiting List";
                        setupLeaveButton(event, deviceId);
                        break;
                    case DatabaseHandler.STATUS_SELECTED:
                    case DatabaseHandler.STATUS_INVITED:
                        acceptButton.setVisibility(View.VISIBLE);
                        declineButton.setVisibility(View.VISIBLE);
                        statusPrefix = "Status: YOU ARE SELECTED!";
                        setupAcceptDeclineButtons(event, deviceId, dbHandler);
                        break;
                    case DatabaseHandler.STATUS_ACCEPTED:
                        leaveButton.setVisibility(View.VISIBLE);
                        statusPrefix = "Status: Accepted";
                        setupLeaveButton(event, deviceId);
                        break;
                    case DatabaseHandler.STATUS_REJECTED:
                        statusPrefix = "Status: Not Selected";
                        break;
                    case DatabaseHandler.STATUS_DECLINED:
                        statusPrefix = "Status: Declined";
                        break;
                }
            }
            updateEntrantCount(event, statusPrefix);
        }

        private void setupJoinButton(Event event, String deviceId) {
            joinButton.setOnClickListener(v -> {
                Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                    event.addEntrant(entrant).addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), "Joined " + event.getName(), Toast.LENGTH_SHORT).show();
                        refreshUI(event, deviceId);
                    });
                });
            });
        }

        private void setupLeaveButton(Event event, String deviceId) {
            leaveButton.setOnClickListener(v -> {
                Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                    event.removeEntrant(entrant).addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), "Left " + event.getName(), Toast.LENGTH_SHORT).show();
                        refreshUI(event, deviceId);
                    });
                });
            });
        }

        private void setupAcceptDeclineButtons(Event event, String deviceId, DatabaseHandler dbHandler) {
            acceptButton.setOnClickListener(v -> {
                Map<String, Object> data = new HashMap<>();
                data.put("status", DatabaseHandler.STATUS_ACCEPTED);
                dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), deviceId, data).addOnSuccessListener(aVoid -> {
                    Toast.makeText(itemView.getContext(), "Accepted!", Toast.LENGTH_SHORT).show();
                    refreshUI(event, deviceId);
                });
            });

            declineButton.setOnClickListener(v -> {
                Map<String, Object> data = new HashMap<>();
                data.put("status", DatabaseHandler.STATUS_DECLINED);
                dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), deviceId, data).addOnSuccessListener(aVoid -> {
                    Toast.makeText(itemView.getContext(), "Declined", Toast.LENGTH_SHORT).show();
                    dbHandler.selectAndInviteNextEntrant(String.valueOf(event.getEventId()));
                    refreshUI(event, deviceId);
                });
            });
        }

        private void refreshUI(Event event, String deviceId) {
            new DatabaseHandler().getEntrantStatus(String.valueOf(event.getEventId()), deviceId).addOnSuccessListener(newStatus -> {
                updateUIBasedOnStatus(newStatus, event, deviceId, new DatabaseHandler());
            });
        }

        private void showDeleteConfirmation(Event event, Runnable onDelete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_delete_event, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            TextView title = dialogView.findViewById(R.id.tv_dialog_title);
            TextView message = dialogView.findViewById(R.id.tv_dialog_message);
            Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
            Button btnDelete = dialogView.findViewById(R.id.btn_dialog_delete);
            ImageView ivClose = dialogView.findViewById(R.id.iv_close);

            if (title != null) title.setText("Delete Event");
            if (message != null)
                message.setText("Are you sure you want to delete " + event.getName() + "?");

            if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
            if (ivClose != null) ivClose.setOnClickListener(v -> dialog.dismiss());

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    new DatabaseHandler().deleteEvent(String.valueOf(event.getEventId())).addOnSuccessListener(aVoid -> {
                        if (onDelete != null) onDelete.run();
                        dialog.dismiss();
                    });
                });
            }

            dialog.show();
        }
    }
}
