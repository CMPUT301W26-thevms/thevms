package com.example.thevms.model;

import com.google.firebase.Timestamp;
import java.util.Date;

/**
 * Model class representing a comment on an event.
 */
public class Comment {
    private String userId;
    private String firstName;
    private String lastName;
    private String text;
    private Date timestamp;

    public Comment() {
        // Required for Firebase
    }

    public Comment(String userId, String firstName, String lastName, String text, Date timestamp) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
