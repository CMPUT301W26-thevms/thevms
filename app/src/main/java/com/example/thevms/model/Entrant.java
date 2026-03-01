package com.example.thevms.model;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an entrant in an event.
 */
public class Entrant {
    protected final DatabaseHandler dbHandler;
    private String deviceId;
    private String email;
    private String username;
    private String phoneNumber;

    public Entrant(String deviceId, String email, String username, String phoneNumber) {
        this.dbHandler = new DatabaseHandler();
        this.deviceId = deviceId;
        this.email = email;
        this.username = username;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Saves the current entrant's profile to the database.
     * @return A Task representing the async operation.
     */
    public Task<Void> save() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("username", username);
        data.put("phoneNumber", phoneNumber);
        return dbHandler.saveUser(deviceId, data);
    }

    /**
     * Creates an Entrant object from a Map of data.
     * @param deviceId The unique device ID.
     * @param data The data map from Firestore.
     * @return A populated Entrant object.
     */
    public static Entrant fromMap(String deviceId, Map<String, Object> data) {
        if (data == null) return null;
        String email = (String) data.get("email");
        String username = (String) data.get("username");
        String phoneNumber = (String) data.get("phoneNumber");
        return new Entrant(deviceId, email, username, phoneNumber);
    }

    /**
     * Fetches a user from the database based on the device ID, or returns a new Entrant if not found.
     * @param deviceId The Android device ID.
     * @return A Task containing the Entrant object.
     */
    public static Task<Entrant> getOrCreate(String deviceId) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        return dbHandler.getUser(deviceId).continueWith(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    return Entrant.fromMap(deviceId, doc.getData());
                } else {
                    // Return a new entrant instance that hasn't been saved yet
                    return new Entrant(deviceId, null, null, null);
                }
            } else {
                throw task.getException();
            }
        });
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void registerInEvent(Event event) {
        event.addEntrant(this, Boolean.FALSE);
    }

    public void unregisterFromEvent(Event event) {
        event.removeEntrant(this);
    }
}
