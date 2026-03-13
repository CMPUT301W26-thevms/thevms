package com.example.thevms.model;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private boolean notificationsEnabled;
    private UserRole role;

    public Entrant(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber) {
        this(deviceId, email, firstName, lastName, phoneNumber, true, UserRole.ENTRANT);
    }

    public Entrant(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber, boolean notificationsEnabled) {
        this.dbHandler = new DatabaseHandler();
        this.deviceId = deviceId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.notificationsEnabled = notificationsEnabled;
        this.role = UserRole.ENTRANT;
    }

    public Entrant(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber, boolean notificationsEnabled, UserRole role) {
        this.dbHandler = new DatabaseHandler();
        this.deviceId = deviceId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.notificationsEnabled = notificationsEnabled;
        this.role = role;
    }

    public Task<Void> save() {
        Map<String, Object> data = toMap();
        return dbHandler.saveUser(deviceId, data);
    }

    protected Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("phoneNumber", phoneNumber);
        data.put("notificationsEnabled", notificationsEnabled);
        data.put("role", role.name());
        return data;
    }

    public static Entrant fromMap(String deviceId, Map<String, Object> data) {
        if (data == null) return null;
        String email = (String) data.get("email");
        String firstName = (String) data.get("firstName");
        String lastName = (String) data.get("lastName");
        String phoneNumber = (String) data.get("phoneNumber");
        Boolean notifications = (Boolean) data.get("notificationsEnabled");
        String roleStr = (String) data.get("role");
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.ENTRANT;
        return new Entrant(deviceId, email, firstName, lastName, phoneNumber, notifications != null ? notifications : true, role);
    }

    public static Task<Entrant> getOrCreate(String deviceId) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        return dbHandler.getUser(deviceId).continueWith(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    return Entrant.fromMap(deviceId, doc.getData());
                } else {
                    return new Entrant(deviceId, null, null, null, null, true, UserRole.ENTRANT);
                }
            } else {
                throw task.getException();
            }
        });
    }

    /**
     * Option B: No-Index Version.
     * Fetches all events and manually filters those where the user is an entrant.
     */
    public Task<List<Event>> getRegisteredEvents() {
        return dbHandler.getAllEvents().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            List<Event> allEvents = new ArrayList<>();
            List<Task<Boolean>> membershipTasks = new ArrayList<>();

            for (DocumentSnapshot doc : task.getResult()) {
                Event event = Event.fromDoc(doc);
                if (event != null) {
                    allEvents.add(event);
                    membershipTasks.add(event.inEvent(this));
                }
            }

            if (allEvents.isEmpty()) {
                return Tasks.forResult(new ArrayList<Event>());
            }

            // Wait for all "is user in this event" checks to complete
            return Tasks.whenAllComplete(membershipTasks).continueWith(checksTask -> {
                List<Event> joinedEvents = new ArrayList<>();
                for (int i = 0; i < membershipTasks.size(); i++) {
                    Task<Boolean> check = membershipTasks.get(i);
                    if (check.isSuccessful() && check.getResult()) {
                        joinedEvents.add(allEvents.get(i));
                    }
                }
                return joinedEvents;
            });
        });
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    @Nullable public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(@Nullable String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public void registerInEvent(Event event) { event.addEntrant(this); }
    public void unregisterFromEvent(Event event) { event.removeEntrant(this); }
}
