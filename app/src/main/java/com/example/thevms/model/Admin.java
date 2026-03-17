package com.example.thevms.model;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents an administrator user with elevated privileges.
 * Inherits from Organizer, allowing them to manage events.
 */
public class Admin extends Organizer {

    /**
     * Constructor for the Admin class with default notification settings.
     *
     * @param deviceId    The admin's unique device ID.
     * @param email       The admin's email address.
     * @param firstName   The admin's first name.
     * @param lastName    The admin's last name.
     * @param phoneNumber The admin's phone number (optional).
     */
    public Admin(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber) {
        this(deviceId, email, firstName, lastName, phoneNumber, true);
    }

    /**
     * Constructor for the Admin class with specified notification settings.
     *
     * @param deviceId             The admin's unique device ID.
     * @param email                The admin's email address.
     * @param firstName            The admin's first name.
     * @param lastName             The admin's last name.
     * @param phoneNumber          The admin's phone number (optional).
     * @param notificationsEnabled Whether notifications are enabled for this admin.
     */
    public Admin(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber, boolean notificationsEnabled) {
        super(deviceId, email, firstName, lastName, phoneNumber, notificationsEnabled);
        this.setRole(UserRole.ADMIN);
    }

    /**
     * Creates an Admin object from a Map of data.
     *
     * @param deviceId The unique device ID.
     * @param data     The data map from Firestore.
     * @return A populated Admin object, or null if data is null.
     */
    public static Admin fromMap(String deviceId, Map<String, Object> data) {
        if (data == null) return null;
        String email = (String) data.get("email");
        String firstName = (String) data.get("firstName");
        String lastName = (String) data.get("lastName");
        String phoneNumber = (String) data.get("phoneNumber");
        Boolean notifications = (Boolean) data.get("notificationsEnabled");
        return new Admin(deviceId, email, firstName, lastName, phoneNumber, notifications != null ? notifications : true);
    }

    /**
     * Removes an event from the system.
     * This operation affects both the global list and the organizer's specific list.
     *
     * @param event     The event to be removed.
     * @param eventList The list of all events in the system.
     */
    public void removeEvent(Event event, List<Event> eventList) {
        // Remove the event from the global list of events
        eventList.remove(event);

        // Remove the event from the organizer's list of events
        if (event.getOrganizer() != null && event.getOrganizer().currentEvents != null) {
            event.getOrganizer().currentEvents.remove(event);
        }
    }
}
