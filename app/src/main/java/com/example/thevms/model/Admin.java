package com.example.thevms.model;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Represents an administrator user.
 */
public class Admin extends Organizer {

    /**
     * Constructor for the Admin class.
     *
     * @param deviceId    The admin's device id.
     * @param email       The admin's email.
     * @param firstName   The admin's first name.
     * @param lastName    The admin's last name.
     * @param phoneNumber The admin's phone number.
     */
    public Admin(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber) {
        super(deviceId, email, firstName, lastName, phoneNumber);
    }

    /**
     * Removes an event from the system.
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
