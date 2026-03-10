package com.example.thevms.ui.Event;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
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
        
        holder.bind(event, dateFormat, fullDateFormat, isAdmin, isExpanded, 
            () -> {
                // Delete callback
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    events.remove(currentPos);
                    notifyItemRemoved(currentPos);
                }
            },
            (expand) -> {
                // Toggle expansion callback
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
        TextView nameTextView;
        TextView statusTextView;
        TextView timeTextView;
        TextView locationTextView;
        Button joinButton;
        Button removeButton;
        Button leaveButton;
        Button acceptButton;
        Button declineButton;
        ImageView eventImageView;

        // Expandable views
        View expandableDetails;
        TextView descriptionTextView;
        TextView regStartTextView;
        TextView regEndTextView;
        TextView eventStartTextView;
        TextView eventEndTextView;
        Button expandButton;

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

            expandableDetails = itemView.findViewById(R.id.expandable_details);
            descriptionTextView = itemView.findViewById(R.id.event_description);
            regStartTextView = itemView.findViewById(R.id.event_reg_start_details);
            regEndTextView = itemView.findViewById(R.id.event_reg_end_details);
            eventStartTextView = itemView.findViewById(R.id.event_start_details);
            eventEndTextView = itemView.findViewById(R.id.event_end_details);
            expandButton = itemView.findViewById(R.id.btn_expand_details);
        }

        public void bind(Event event, SimpleDateFormat dateFormat, SimpleDateFormat fullDateFormat, 
                         boolean isAdmin, boolean isExpanded, Runnable onDelete, 
                         java.util.function.Consumer<Boolean> onToggleExpand) {
            
            if (nameTextView != null) nameTextView.setText(event.getName());

            updateEntrantCount(event);

            if (timeTextView != null) {
                if (event.getEventStartTime() != null) {
                    timeTextView.setText(String.format("Starts %s", dateFormat.format(event.getEventStartTime())));
                } else {
                    timeTextView.setText("Time TBD");
                }
            }

            if (locationTextView != null) {
                locationTextView.setText("📍 Nearby");
            }

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

            @SuppressLint("HardwareIds")
            String deviceId = Settings.Secure.getString(itemView.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

            DatabaseHandler dbHandler = new DatabaseHandler();
            dbHandler.getEntrantStatus(String.valueOf(event.getEventId()), deviceId).addOnSuccessListener(status -> {
                updateUIBasedOnStatus(status, event);
            });

            // Handle Join Button
            if (joinButton != null) {
                joinButton.setOnClickListener(v -> {
                    Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                        event.addEntrant(entrant).addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "Successfully joined " + event.getName(), Toast.LENGTH_SHORT).show();
                            updateEntrantCount(event);
                            updateUIBasedOnStatus("waiting", event);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(itemView.getContext(), "Failed to join event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "Error retrieving user profile", Toast.LENGTH_SHORT).show();
                    });
                });
            }

            // Handle Leave Button
            if (leaveButton != null) {
                leaveButton.setOnClickListener(v -> {
                    Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                        event.removeEntrant(entrant).addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "Successfully left " + event.getName(), Toast.LENGTH_SHORT).show();
                            updateEntrantCount(event);
                            updateUIBasedOnStatus(null, event);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(itemView.getContext(), "Failed to leave event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(), "Error retrieving user profile", Toast.LENGTH_SHORT).show();
                    });
                });
            }

            // Handle Accept Button
            if (acceptButton != null) {
                acceptButton.setOnClickListener(v -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "accepted");
                    dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), deviceId, data).addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), "Accepted invitation for " + event.getName(), Toast.LENGTH_SHORT).show();
                        updateUIBasedOnStatus("accepted", event);
                    });
                });
            }

            // Handle Decline Button
            if (declineButton != null) {
                declineButton.setOnClickListener(v -> {
                    Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                        event.removeEntrant(entrant).addOnSuccessListener(aVoid -> {
                            Toast.makeText(itemView.getContext(), "Declined invitation for " + event.getName(), Toast.LENGTH_SHORT).show();
                            updateEntrantCount(event);
                            updateUIBasedOnStatus(null, event);
                        });
                    });
                });
            }

            // Handle Admin/Remove Button
            if (removeButton != null) {
                removeButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                removeButton.setOnClickListener(v -> showDeleteConfirmation(event, onDelete));
            }
        }

        private void updateUIBasedOnStatus(String status, Event event) {
            if (status == null) {
                joinButton.setVisibility(View.VISIBLE);
                leaveButton.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
                declineButton.setVisibility(View.GONE);
                statusTextView.setText("Not joined");
            } else {
                joinButton.setVisibility(View.GONE);
                if ("waiting".equals(status)) {
                    leaveButton.setVisibility(View.VISIBLE);
                    acceptButton.setVisibility(View.GONE);
                    declineButton.setVisibility(View.GONE);
                    statusTextView.setText("Status: Waiting List");
                } else if ("selected".equals(status) || "invited".equals(status)) {
                    leaveButton.setVisibility(View.GONE);
                    acceptButton.setVisibility(View.VISIBLE);
                    declineButton.setVisibility(View.VISIBLE);
                    statusTextView.setText("Status: YOU ARE SELECTED!");
                } else if ("accepted".equals(status)) {
                    leaveButton.setVisibility(View.VISIBLE); // Can still leave? or just stay accepted? Requirement says "Leave" for all signed up
                    acceptButton.setVisibility(View.GONE);
                    declineButton.setVisibility(View.GONE);
                    statusTextView.setText("Status: Accepted");
                } else if ("rejected".equals(status)) {
                    leaveButton.setVisibility(View.VISIBLE);
                    acceptButton.setVisibility(View.GONE);
                    declineButton.setVisibility(View.GONE);
                    statusTextView.setText("Status: Not Selected");
                }
            }
            // Optionally update entrant count if needed, but usually we want to show total joined
            updateEntrantCount(event);
        }

        private void updateEntrantCount(Event event) {
            if (statusTextView == null) return;
            String currentStatusText = statusTextView.getText().toString();
            event.fetchEntrantCount().addOnSuccessListener(count -> {
                if (currentStatusText.startsWith("Status:")) {
                     statusTextView.setText(currentStatusText + " (Total: " + count + ")");
                } else {
                     statusTextView.setText(String.format(Locale.getDefault(), "☆ %d people joined", count));
                }
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

            String formattedTitle = itemView.getContext().getString(R.string.delete_event_question_formatted, event.getName());
            title.setText(Html.fromHtml(formattedTitle, Html.FROM_HTML_MODE_LEGACY));
            message.setText(R.string.delete_event_consequence);

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            ivClose.setOnClickListener(v -> dialog.dismiss());

            btnDelete.setOnClickListener(v -> {
                deleteEventFromDb(event, onDelete);
                dialog.dismiss();
            });

            dialog.show();
        }

        private void deleteEventFromDb(Event event, Runnable onDelete) {
            DatabaseHandler dbHandler = new DatabaseHandler();
            dbHandler.deleteEvent(String.valueOf(event.getEventId()))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), "Event removed", Toast.LENGTH_SHORT).show();
                        if (onDelete != null) {
                            onDelete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventAdapter", "Error deleting event", e);
                        Toast.makeText(itemView.getContext(), "Failed to remove event", Toast.LENGTH_LONG).show();
                    });
        }
    }
}
