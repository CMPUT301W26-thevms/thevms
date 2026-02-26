package com.example.thevms.model;

import java.util.List;

/**
 * Represents an administrator user.
 */
public class Admin extends Organizer {

    /**
     * Constructor for the Admin class.
     * @param email The admin's email.
     * @param username The admin's username.
     * @param phoneNumber The admin's phone number.
     */
    public Admin(String email, String username, String phoneNumber) {
        super(email, username, phoneNumber);
    }

    /**
     * Removes an event from the system.
     * @param event The event to be removed.
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
