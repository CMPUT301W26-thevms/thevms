package com.example.thevms.model;

import com.google.firebase.Timestamp;
import java.util.Date;

/**
 * Model class representing a comment on an event.
 */
public class Comment {
    private String userId;
    private String userName;
    private String text;
    private Date timestamp;

    public Comment() {
        // Required for Firebase
    }

    public Comment(String userId, String userName, String text, Date timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
