package com.example.thevms.model;

import androidx.annotation.Nullable;

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
    private String firstName;
    private String lastName;
    @Nullable
    private String phoneNumber;

    public Entrant(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber) {
        this.dbHandler = new DatabaseHandler();
        this.deviceId = deviceId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Saves the current entrant's profile to the database.
     * @return A Task representing the async operation.
     */
    public Task<Void> save() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
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
        String firstName = (String) data.get("firstName");
        String lastName = (String) data.get("lastName");
        String phoneNumber = (String) data.get("phoneNumber");
        return new Entrant(deviceId, email, firstName, lastName, phoneNumber);
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
                    return new Entrant(deviceId, null, null, null, null);
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

    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
