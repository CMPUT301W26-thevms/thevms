package com.example.thevms.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Notification;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying notifications in a RecyclerView.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications = new ArrayList<>();
    private OnNotificationDeleteListener deleteListener;
    private OnInviteActionListener inviteActionListener;
    private boolean isReadOnly = false;

    public interface OnNotificationDeleteListener {
        void onDelete(Notification notification);
    }

    public interface OnInviteActionListener {
        void onAccept(Notification notification);

        void onReject(Notification notification);
    }

    public NotificationAdapter() {
        this(false);
    }

    public NotificationAdapter(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public void setOnNotificationDeleteListener(OnNotificationDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnInviteActionListener(OnInviteActionListener listener) {
        this.inviteActionListener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, from, to, description, time;
        ImageButton btnDelete;
        LinearLayout inviteActionsContainer;
        MaterialButton btnAccept, btnReject;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            from = itemView.findViewById(R.id.notification_from);
            to = itemView.findViewById(R.id.notification_to);
            description = itemView.findViewById(R.id.notification_description);
            time = itemView.findViewById(R.id.notification_time);
            btnDelete = itemView.findViewById(R.id.btn_delete_notification);
            inviteActionsContainer = itemView.findViewById(R.id.invite_actions_container);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }

        public void bind(Notification notification) {
            title.setText(notification.getTitle() != null ? notification.getTitle() : "Notification - !!ERROR BAD NOTIF!!");

            String nameStr = (notification.getSenderName() != null) ? notification.getSenderName() : "System";
            String roleStr = (notification.getSenderRole() != null) ? notification.getSenderRole().toString() : "ADMIN";
            String senderInfo = "From: " + nameStr + " (" + roleStr + ")";
            from.setText(senderInfo);

            if (notification.getDescription() != null && !notification.getDescription().trim().isEmpty()) {
                description.setText(notification.getDescription());
            } else {
                description.setText("This is not how you create a notification please use one of the create notification methods or make a new create notification method.");
            }

            if (notification.getTimestamp() != null) {
                time.setText(DateFormat.format("MMM dd, h:mm a", notification.getTimestamp()));
            } else {
                time.setText("");
            }

            if (isReadOnly) {
                btnDelete.setVisibility(View.GONE);
                inviteActionsContainer.setVisibility(View.GONE);

                // Admin logs view: show who the notification was sent to
                to.setVisibility(View.VISIBLE);
                String receiverName = (notification.getReceiverName() != null) ? notification.getReceiverName() : "Unknown Recipient";
                to.setText("To: " + receiverName);
            } else {
                btnDelete.setVisibility(View.VISIBLE);
                to.setVisibility(View.GONE); // Inbox view: don't show recipient

                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDelete(notification);
                    }
                });

                // Handle invite actions visibility
                if (Notification.TYPE_INVITE.equals(notification.getType()) || Notification.TYPE_SELECTED.equals(notification.getType())) {
                    inviteActionsContainer.setVisibility(View.VISIBLE);
                    btnAccept.setOnClickListener(v -> {
                        if (inviteActionListener != null) {
                            inviteActionListener.onAccept(notification);
                        }
                    });
                    btnReject.setOnClickListener(v -> {
                        if (inviteActionListener != null) {
                            inviteActionListener.onReject(notification);
                        }
                    });
                } else {
                    inviteActionsContainer.setVisibility(View.GONE);
                }
            }
        }
    }
}
