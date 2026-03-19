package com.example.thevms.model;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Model class representing a notification in the system.
 * Notifications are sent by organizers or the system to users (entrants).
 */
public class Notification {
    public static final String TYPE_GENERAL = "general";
    public static final String TYPE_INVITE = "invite";

    private String id;
    private String title;
    private String senderId;
    private String senderName;
    private UserRole senderRole;
    private String receiverId;
    private Date timestamp;
    private String description;
    private String type;
    private boolean isRead;

    @Exclude // Exclude is used so that it is not saved to the DB
    private final DatabaseHandler dbHandler;

    /**
     * Default constructor required for Firebase Firestore deserialization.
     */
    public Notification() {
        this.dbHandler = new DatabaseHandler();
        this.isRead = false;
        this.type = TYPE_GENERAL;
    }

    /**
     * Constructs a new Notification.
     *
     * @param id          The unique identifier for the notification (usually Firestore document ID).
     * @param title       The headline of the notification.
     * @param senderId    The device ID or user ID of the sender.
     * @param senderName  The display name of the person or system sending the notification.
     * @param senderRole  The role of the sender (e.g., ADMIN, ORGANIZER).
     * @param receiverId  The device ID of the user who should receive this notification.
     * @param timestamp   The date and time the notification was generated.
     * @param description The main content/message of the notification.
     */
    public Notification(String id, String title, String senderId, String senderName, UserRole senderRole, String receiverId, Date timestamp, String description) {
        this(id, title, senderId, senderName, senderRole, receiverId, timestamp, description, TYPE_GENERAL);
    }

    /**
     * Constructs a new Notification with a specific type.
     *
     * @param id          The unique identifier for the notification.
     * @param title       The headline of the notification.
     * @param senderId    The device ID of the sender.
     * @param senderName  The display name of the sender.
     * @param senderRole  The role of the sender.
     * @param receiverId  The device ID of the receiver.
     * @param timestamp   The date and time of the notification.
     * @param description The main content of the notification.
     * @param type        The type of notification (e.g., general, invite).
     */
    public Notification(String id, String title, String senderId, String senderName, UserRole senderRole, String receiverId, Date timestamp, String description, String type) {
        this.dbHandler = new DatabaseHandler();
        this.id = id;
        this.title = title;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.description = description;
        this.type = type;
        this.isRead = false;
    }

    /**
     * Sends this notification using the internal DatabaseHandler.
     *
     * @return A Task representing the asynchronous send operation.
     */
    public Task<Void> send() {
        return dbHandler.sendNotification(this);
    }

    /**
     * Deletes this notification using the internal DatabaseHandler.
     *
     * @return A Task representing the asynchronous deletion operation.
     */
    public Task<Void> delete() {
        return dbHandler.deleteNotification(id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public UserRole getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(UserRole senderRole) {
        this.senderRole = senderRole;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
