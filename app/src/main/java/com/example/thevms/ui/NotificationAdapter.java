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

    public interface OnNotificationDeleteListener {
        void onDelete(Notification notification);
    }

    public interface OnInviteActionListener {
        void onAccept(Notification notification);
        void onReject(Notification notification);
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
        TextView title, from, description, time;
        ImageButton btnDelete;
        LinearLayout inviteActionsContainer;
        MaterialButton btnAccept, btnReject;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            from = itemView.findViewById(R.id.notification_from);
            description = itemView.findViewById(R.id.notification_description);
            time = itemView.findViewById(R.id.notification_time);
            btnDelete = itemView.findViewById(R.id.btn_delete_notification);
            inviteActionsContainer = itemView.findViewById(R.id.invite_actions_container);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }

        public void bind(Notification notification) {
            title.setText(notification.getTitle());
            String senderInfo = "From: " + notification.getSenderName() + " (" + notification.getSenderRole().toString() + ")";
            from.setText(senderInfo);
            description.setText(notification.getDescription());
            
            if (notification.getTimestamp() != null) {
                time.setText(DateFormat.format("MMM dd, h:mm a", notification.getTimestamp()));
            }

            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(notification);
                }
            });

            // Handle invite actions visibility
            if (Notification.TYPE_INVITE.equals(notification.getType())) {
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
