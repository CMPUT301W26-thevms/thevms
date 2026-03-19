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
                Toast.makeText(getContext(), "Accepted Invite: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                // Logic for accepting invite (e.g. updating entrant status) will be added later
            }

            @Override
            public void onReject(Notification notification) {
                Toast.makeText(getContext(), "Rejected Invite: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                // Logic for rejecting invite will be added later
            }
        });
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
            Notification testInvite = new Notification(
                    null,
                    "Event Invitation",
                    deviceId,
                    "Event Organizer",
                    UserRole.ORGANIZER,
                    deviceId,
                    new Date(),
                    "You have been selected for the upcoming event! Would you like to attend?",
                    Notification.TYPE_INVITE
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
