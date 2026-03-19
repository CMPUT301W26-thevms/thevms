package com.example.thevms.model;

import java.util.Date;

/**
 * Model class representing a notification in the system.
 * Notifications are sent by organizers or the system to users (entrants).
 */
public class Notification {
    private String id;
    private String title;
    private String senderId;
    private String senderName;
    private UserRole senderRole;
    private String receiverId;
    private Date timestamp;
    private String description;
    private boolean isRead;

    /**
     * Default constructor required for Firebase Firestore deserialization.
     */
    public Notification() {
        // Required for Firebase
    }

    /**
     * Constructs a new Notification.
     *
     * @param id           The unique identifier for the notification (usually Firestore document ID).
     * @param title        The headline of the notification.
     * @param senderId     The device ID or user ID of the sender.
     * @param senderName   The display name of the person or system sending the notification.
     * @param senderRole   The role of the sender (e.g., ADMIN, ORGANIZER).
     * @param receiverId   The device ID of the user who should receive this notification.
     * @param timestamp    The date and time the notification was generated.
     * @param description  The main content/message of the notification.
     */
    public Notification(String id, String title, String senderId, String senderName, UserRole senderRole, String receiverId, Date timestamp, String description) {
        this.id = id;
        this.title = title;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.description = description;
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public UserRole getSenderRole() { return senderRole; }
    public void setSenderRole(UserRole senderRole) { this.senderRole = senderRole; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
