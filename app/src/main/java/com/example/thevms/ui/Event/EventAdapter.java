package com.example.thevms.ui.Event;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Comment;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for displaying a list of events in a RecyclerView.
 * Handles event binding, expansion, and user interactions such as joining or leaving an event.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private boolean isAdmin = false;
    private boolean managementMode = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final Set<Long> expandedEventIds = new HashSet<>();
    private final Set<Long> expandedCommentIds = new HashSet<>();
    public static android.location.Location testLocation = null;

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
     * Sets whether the current user has administrative privileges.
     *
     * @param admin True if the user is an admin, false otherwise.
     */
    public void setAdmin(boolean admin) {
        isAdmin = admin;
        notifyDataSetChanged();
    }

    /**
     * Sets whether the adapter is in management mode (e.g., for organizers).
     *
     * @param managementMode True if in management mode, false otherwise.
     */
    public void setManagementMode(boolean managementMode) {
        this.managementMode = managementMode;
        notifyDataSetChanged();
    }

    /**
     * Sets a test location for geolocation checks.
     *
     * @param location The location to use for testing.
     */
    public static void setTestLocation(android.location.Location location) {
        testLocation = location;
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
        boolean isCommentsExpanded = expandedCommentIds.contains(event.getEventId());

        holder.bind(event, dateFormat, fullDateFormat, isAdmin, managementMode, isExpanded, isCommentsExpanded,
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
                },
                (expandComments) -> {
                    if (!expandComments) {
                        // Clear focus and hide keyboard before hiding the layout
                        if (holder.commentEditText.hasFocus()) {
                            holder.commentEditText.clearFocus();
                            InputMethodManager imm = (InputMethodManager) holder.itemView.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(holder.commentEditText.getWindowToken(), 0);
                            }
                        }
                    }
                    if (expandComments) {
                        expandedCommentIds.add(event.getEventId());
                    } else {
                        expandedCommentIds.remove(event.getEventId());
                    }
                    notifyItemChanged(holder.getAdapterPosition());
                }
        );
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class for individual event items.
     * Manages the UI components and user actions for a single event card.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
        TextView nameTextView, statusTextView, timeTextView, locationTextView, waitlistTextView;
        Button joinButton, removeButton, leaveButton, acceptButton, declineButton, expandButton, viewCommentsButton, postCommentButton;
        ImageView eventImageView;
        TextView regStatusMessage, descriptionTextView, regStartTextView, regEndTextView, eventStartTextView, eventEndTextView;
        View expandableDetails, commentsSection;
        RecyclerView commentsRecyclerView;
        EditText commentEditText;
        CommentAdapter commentAdapter;
        ListenerRegistration commentsListener;

        /**
         * Initializes the ViewHolder with the item view and finds all necessary subviews.
         *
         * @param itemView The view representing a single event item.
         */
        @SuppressLint("ClickableViewAccessibility")
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.event_name);
            statusTextView = itemView.findViewById(R.id.event_status_info);
            waitlistTextView = itemView.findViewById(R.id.event_waitlist_info);
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
            
            viewCommentsButton = itemView.findViewById(R.id.btn_view_comments);
            commentsSection = itemView.findViewById(R.id.comments_section);
            commentsRecyclerView = itemView.findViewById(R.id.comments_recycler_view);
            commentEditText = itemView.findViewById(R.id.edit_comment);
            postCommentButton = itemView.findViewById(R.id.btn_post_comment);

            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            commentAdapter = new CommentAdapter();
            commentsRecyclerView.setAdapter(commentAdapter);

            // Fix for nested scrolling: allow the comments RecyclerView to intercept touch events
            commentsRecyclerView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });

            fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(itemView.getContext());
        }

        /**
         * Binds an event to the ViewHolder, setting text and visibility for various UI elements.
         *
         * @param event          The event to bind.
         * @param dateFormat     Formatter for short dates.
         * @param fullDateFormat Formatter for full date and time.
         * @param isAdmin        Whether the user is an admin.
         * @param managementMode Whether the adapter is in management mode.
         * @param isExpanded     Whether this item's details are currently expanded.
         * @param isCommentsExpanded Whether this item's comments are currently expanded.
         * @param onDelete       Callback for when an event is deleted.
         * @param onToggleExpand Callback for toggling the expansion state.
         * @param onToggleComments Callback for toggling the comments expansion state.
         */
        public void bind(Event event, SimpleDateFormat dateFormat, SimpleDateFormat fullDateFormat, 
                         boolean isAdmin, boolean managementMode, boolean isExpanded, boolean isCommentsExpanded,
                         Runnable onDelete,
                         java.util.function.Consumer<Boolean> onToggleExpand,
                         java.util.function.Consumer<Boolean> onToggleComments) {

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

            if (locationTextView != null) {
                locationTextView.setText(event.getLocation());
            }

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

            // Handle Comments state
            if (commentsSection != null) {
                commentsSection.setVisibility(isCommentsExpanded ? View.VISIBLE : View.GONE);
            }
            if (viewCommentsButton != null) {
                viewCommentsButton.setText(isCommentsExpanded ? "Hide Comments" : "View Comments");
                viewCommentsButton.setOnClickListener(v -> onToggleComments.accept(!isCommentsExpanded));
            }

            if (isCommentsExpanded) {
                setupComments(String.valueOf(event.getEventId()), deviceId, dbHandler, isAdmin);
            } else {
                if (commentsListener != null) {
                    commentsListener.remove();
                    commentsListener = null;
                }
            }
        }

        private void setupComments(String eventId, String deviceId, DatabaseHandler dbHandler, boolean isAdmin) {
            if (commentsListener != null) {
                commentsListener.remove();
            }
            
            commentAdapter.setShowDeleteButton(isAdmin);
            
            commentsListener = dbHandler.listenToComments(eventId, (value, error) -> {
                if (error != null) {
                    Log.w("EventAdapter", "Listen failed.", error);
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
                if (comments.size() > 0 && commentsRecyclerView.getVisibility() == View.VISIBLE) {
                    commentsRecyclerView.smoothScrollToPosition(comments.size() - 1);
                }
            });

            if (isAdmin) {
                commentAdapter.setOnCommentDeleteListener((comment, commentId) -> {
                    showDeleteCommentConfirmation(eventId, commentId);
                });
            }

            postCommentButton.setOnClickListener(v -> {
                String text = commentEditText.getText().toString().trim();
                if (text.isEmpty()) return;

                dbHandler.getUser(deviceId).addOnSuccessListener(userDoc -> {
                    String firstName = userDoc.getString("firstName");
                    String lastName = userDoc.getString("lastName");
                    if (firstName == null || firstName.isEmpty()) {
                        firstName = "Anonymous";
                    }
                    if (lastName == null) {
                        lastName = "";
                    }
                    Comment comment = new Comment(deviceId, firstName, lastName, text, new Date());
                    dbHandler.addComment(eventId, comment).addOnSuccessListener(aVoid -> {
                        commentEditText.setText("");
                    });
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
                    new DatabaseHandler().deleteComment(eventId, commentId).addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(), R.string.comment_deleted_toast, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            }

            dialog.show();
        }

        /**
         * Updates the entrant count text and waitlist information.
         *
         * @param event        The event to fetch information for.
         * @param statusPrefix An optional status message to display before the count.
         */
        private void updateEntrantInfo(Event event, String statusPrefix) {
            if (statusTextView == null) return;
            event.fetchEntrantCount().addOnSuccessListener(count -> {
                String text = String.format(Locale.getDefault(), "☆ %d people joined", count);
                if (statusPrefix != null && !statusPrefix.isEmpty()) {
                    text = statusPrefix + " (" + text + ")";
                }
                statusTextView.setText(text);

                if (waitlistTextView != null) {
                    Integer maxWaitlist = event.getMaxWaitlist();
                    if (maxWaitlist != null) {
                        waitlistTextView.setVisibility(View.VISIBLE);
                        waitlistTextView.setText(String.format(Locale.getDefault(), "Waitlist: %d/%d", count, maxWaitlist));
                        
                        // Grey out join button if waitlist is full
                        if (count >= maxWaitlist && joinButton.getVisibility() == View.VISIBLE) {
                            joinButton.setEnabled(false);
                            joinButton.setAlpha(0.5f);
                            joinButton.setText("Waitlist Full");
                        } else {
                            joinButton.setEnabled(true);
                            joinButton.setAlpha(1.0f);
                            joinButton.setText("Join");
                        }
                    } else {
                        waitlistTextView.setVisibility(View.GONE);
                        joinButton.setEnabled(true);
                        joinButton.setAlpha(1.0f);
                        joinButton.setText("Join");
                    }
                }
            }).addOnFailureListener(e -> {
                statusTextView.setText("☆ -- people joined");
            });
        }

        /**
         * Updates the UI buttons and status text based on the user's status for the event.
         *
         * @param status    The user's status (e.g., waiting, invited, accepted).
         * @param event     The event in question.
         * @param deviceId  The unique ID of the device.
         * @param dbHandler The database handler to use for updates.
         */
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
            updateEntrantInfo(event, statusPrefix);
        }

        /**
         * Sets up the join button with appropriate visibility and click listener.
         *
         * @param event    The event to join.
         * @param deviceId The unique ID of the device.
         */
        private void setupJoinButton(Event event, String deviceId){
            Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                event.inEvent(entrant).addOnSuccessListener(isIn -> {
                    if (joinButton != null) {
                        joinButton.setVisibility(isIn ? View.GONE : View.VISIBLE);
                        // If user is already in event, hide the "not started/ended" message too
                        if (isIn && regStatusMessage != null) {
                            regStatusMessage.setVisibility(View.GONE);
                        }
                    }
                    if (leaveButton != null) {
                        leaveButton.setVisibility(isIn ? View.VISIBLE : View.GONE);
                    }
                });
            });
            if (joinButton != null) {
                joinButton.setOnClickListener(v -> checkLocation(event, deviceId));
            }

        }

        /**
         * Sets up the leave button with appropriate click listener.
         *
         * @param event    The event to leave.
         * @param deviceId The unique ID of the device.
         */
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

        /**
         * Sets up the accept and decline buttons for invited users.
         *
         * @param event     The event to accept or decline.
         * @param deviceId  The unique ID of the device.
         * @param dbHandler The database handler to use for updates.
         */
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

        /**
         * Refreshes the UI for a specific event by fetching the current user's status.
         *
         * @param event    The event to refresh.
         * @param deviceId The unique ID of the device.
         */
        private void refreshUI(Event event, String deviceId) {
            new DatabaseHandler().getEntrantStatus(String.valueOf(event.getEventId()), deviceId).addOnSuccessListener(newStatus -> {
                updateUIBasedOnStatus(newStatus, event, deviceId, new DatabaseHandler());
            });
        }

        /**
         * Handles the logic for a user joining an event.
         *
         * @param event    The event to join.
         * @param deviceId The unique ID of the device.
         */
        private void joinEvent(Event event, String deviceId) {
            Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                event.addEntrant(entrant).addOnSuccessListener(aVoid -> {
                    Toast.makeText(itemView.getContext(), "Successfully joined " + event.getName(), Toast.LENGTH_SHORT).show();
                    updateEntrantInfo(event, null);
                    joinButton.setVisibility(View.GONE);
                    leaveButton.setVisibility(View.VISIBLE);
                    if (regStatusMessage != null) {
                        regStatusMessage.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(itemView.getContext(), "Failed to join event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(itemView.getContext(), "Error retrieving user profile", Toast.LENGTH_SHORT).show();
            });
        }

        /**
         * Compares the user's current location with the event's location requirement.
         *
         * @param currentLoc The user's current location.
         * @param event      The event being joined.
         * @param deviceId   The unique ID of the device.
         */
        private void handleLocationResult(android.location.Location currentLoc, Event event, String deviceId) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    currentLoc.getLatitude(), currentLoc.getLongitude(),
                    event.getGeoLocation().getLatitude(), event.getGeoLocation().getLongitude(),
                    results);

            float distanceInMeters = results[0];
            double radiusInMeters = event.getRadius() * 1000;

            if (distanceInMeters <= radiusInMeters) {
                joinEvent(event, deviceId);
            } else {
                Toast.makeText(itemView.getContext(),
                        "Out of location requirement", Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Checks if the user is within the required geolocation for the event before joining.
         *
         * @param event    The event to check location for.
         * @param deviceId The unique ID of the device.
         */
        private void checkLocation(Event event, String deviceId) {
            if (!event.isGeolocationRequired()) {
                joinEvent(event, deviceId);
                return;
            }

            if (EventAdapter.testLocation != null) {
                handleLocationResult(EventAdapter.testLocation, event, deviceId);
                return;
            }

            if (event.getGeoLocation() == null) {
                Log.e("JOIN_ERROR", "Event coordinates are missing in database!");
                Toast.makeText(itemView.getContext(), "Error: Event has no location data.", Toast.LENGTH_LONG).show();
                return;
            }

            if (androidx.core.content.ContextCompat.checkSelfPermission(itemView.getContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (itemView.getContext() instanceof android.app.Activity) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                            (android.app.Activity) itemView.getContext(),
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            77
                    );
                    Toast.makeText(itemView.getContext(), "Please give permission to access your location.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            Toast.makeText(itemView.getContext(), "Checking your location...", Toast.LENGTH_SHORT).show();
            com.google.android.gms.location.LocationRequest locationRequest =
                    new com.google.android.gms.location.LocationRequest.Builder(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000)
                            .setMaxUpdates(1)
                            .build();
            com.google.android.gms.location.LocationCallback locationCallback = new com.google.android.gms.location.LocationCallback() {
                @Override
                public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                    fusedLocationClient.removeLocationUpdates(this);

                    android.location.Location currentLoc = locationResult.getLastLocation();
                    if (currentLoc != null) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(
                                currentLoc.getLatitude(), currentLoc.getLongitude(),
                                event.getGeoLocation().getLatitude(), event.getGeoLocation().getLongitude(),
                                results);

                        float distanceInMeters = results[0];
                        double radiusInMeters = event.getRadius() * 1000;

                        if (distanceInMeters <= radiusInMeters) {
                            joinEvent(event, deviceId);
                        } else {
                            Toast.makeText(itemView.getContext(), "Out of location requirement", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(itemView.getContext(), "Still cannot get location", Toast.LENGTH_SHORT).show();
                    }
                }
                };


            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
            );
        }

        /**
         * Shows a confirmation dialog before deleting an event.
         *
         * @param event    The event to be deleted.
         * @param onDelete Callback for when the event is successfully deleted.
         */
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
