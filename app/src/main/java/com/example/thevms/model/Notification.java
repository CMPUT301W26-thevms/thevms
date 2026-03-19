package com.example.thevms.model;

import java.util.Date;

/**
 * Model class representing a notification in the system.
 */
public class Notification {
    private String id;
    private String title;
    private String senderId;
    private String receiverId;
    private UserRole senderRole;
    private Date timestamp;
    private String description;
    private boolean isRead;

    public Notification() {
        // Required for Firebase
    }

    public Notification(String id, String title, String senderId, UserRole senderRole, String receiverId, Date timestamp, String description) {
        this.id = id;
        this.title = title;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.description = description;
        this.isRead = false;
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

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
