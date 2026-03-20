package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Notification;
import com.example.thevms.model.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Fragment representing the inbox section of the app where notifications are displayed.
 */
public class InboxFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private TextView emptyInboxText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseHandler dbHandler;
    private ListenerRegistration notificationsListener;
    private String deviceId;
    private MaterialButton btnTestNotification;
    private MaterialButton btnTestInvite;
    private boolean isInitialLoad = true;

    public InboxFragment() {
        // Required empty public constructor
    }

    @SuppressLint("HardwareIds")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        emptyInboxText = view.findViewById(R.id.empty_inbox_text);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        btnTestNotification = view.findViewById(R.id.btn_test_notification);
        btnTestInvite = view.findViewById(R.id.btn_test_invite);

        dbHandler = new DatabaseHandler();
        deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        setupRecyclerView();
        setupTestButtons();
        setupSwipeRefresh();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnNotificationDeleteListener(this::showDeleteConfirmationDialog);

        adapter.setOnInviteActionListener(new NotificationAdapter.OnInviteActionListener() {
            @Override
            public void onAccept(Notification notification) {
                handleInviteAction(notification, true);
            }

            @Override
            public void onReject(Notification notification) {
                handleInviteAction(notification, false);
            }
        });
    }

    /**
     * Handles the accept/reject logic for an invite notification.
     *
     * @param notification The notification being acted upon.
     * @param isAccepted   True if the user clicked Accept, false for Reject.
     */
    private void handleInviteAction(Notification notification, boolean isAccepted) {
        String eventId = notification.getEventId();
        if (eventId == null) {
            Toast.makeText(getContext(), "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAccepted) {
            if ("Wait List Invite".equals(notification.getTitle())) {
                // For private waitlist invites, we add the entrant to the event's waitlist
                Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
                    dbHandler.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                            .document(eventId).get().addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    // Manually add to waitlist bypassing registration time checks for direct invites
                                    java.util.Map<String, Object> registrationData = new java.util.HashMap<>();
                                    registrationData.put("entrantId", deviceId);
                                    registrationData.put("status", DatabaseHandler.STATUS_WAITING);
                                    registrationData.put("registrationTime", new Date());

                                    dbHandler.updateEntrantStatus(eventId, deviceId, registrationData)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "Joined waiting list!", Toast.LENGTH_SHORT).show();
                                                notification.delete();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            });
                });
            } else if ("Lottery Results".equals(notification.getTitle())) {
                // For lottery wins, update status from 'selected' to 'accepted'
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("status", DatabaseHandler.STATUS_ACCEPTED);
                dbHandler.updateEntrantStatus(eventId, deviceId, data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Invitation accepted!", Toast.LENGTH_SHORT).show();
                            notification.delete();
                        });
            } else {
                // Generic accept
                Toast.makeText(getContext(), "Accepted: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                notification.delete();
            }
        } else {
            // Reject logic
            if ("Lottery Results".equals(notification.getTitle())) {
                // Update status to 'declined' (so organizer can redraw)
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("status", DatabaseHandler.STATUS_DECLINED);
                dbHandler.updateEntrantStatus(eventId, deviceId, data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
                            notification.delete();
                        });
            } else {
                Toast.makeText(getContext(), "Rejected: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                notification.delete();
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting a notification.
     *
     * @param notification The notification to be deleted.
     */
    private void showDeleteConfirmationDialog(Notification notification) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_notification);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivClose = dialog.findViewById(R.id.iv_close);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnDelete = dialog.findViewById(R.id.btn_dialog_delete);

        ivClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            notification.delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Notification deleted", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    private void setupTestButtons() {
        btnTestNotification.setOnClickListener(v -> {
            Notification testNotif = new Notification(
                    null,
                    "Test Notification",
                    deviceId,
                    "Test System",
                    UserRole.ADMIN,
                    deviceId,
                    new Date(),
                    "This is a test notification sent to yourself to verify the inbox functionality."
            );

            testNotif.send()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Test notification sent!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnTestInvite.setOnClickListener(v -> {
            // Using a dummy event ID for testing
            Notification testInvite = Notification.createWaitingListInvite(
                    deviceId, "Test Organizer", deviceId, "123", "Secret Gala"
            );

            testInvite.send()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Test invite sent!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (notificationsListener != null) {
                notificationsListener.remove();
            }
            isInitialLoad = true;
            listenToNotifications();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        isInitialLoad = true;
        listenToNotifications();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }

    private void listenToNotifications() {
        if (isInitialLoad && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }

        notificationsListener = dbHandler.listenToNotifications(deviceId, (value, error) -> {
            if (error != null) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (isInitialLoad) {
                swipeRefreshLayout.setRefreshing(false);
                isInitialLoad = false;
            }

            if (value != null) {
                List<Notification> notifications = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            notifications.add(notification);
                        }
                    } catch (Exception e) {
                        // Skip malformed documents
                    }
                }

                // Sort by timestamp descending (most recent first)
                Collections.sort(notifications, (n1, n2) -> {
                    if (n1.getTimestamp() == null || n2.getTimestamp() == null) return 0;
                    return n2.getTimestamp().compareTo(n1.getTimestamp());
                });

                adapter.setNotifications(notifications);
                updateEmptyState(notifications.isEmpty());
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyInboxText.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
        } else {
            emptyInboxText.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
        }
    }
}
