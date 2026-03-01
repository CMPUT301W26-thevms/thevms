package com.example.thevms.model;

import android.location.Location;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an event organizer.
 */
public class Organizer extends Entrant {
    List<Event> currentEvents;

    /**
     * Constructor for the Organizer class.
     * @param deviceId The organizer's device ID.
     * @param email The organizer's email.
     * @param username The organizer's username.
     * @param phoneNumber The organizer's phone number.
     */
    public Organizer(String deviceId, String email, String username, String phoneNumber) {
        super(deviceId, email, username, phoneNumber);
        this.currentEvents = new ArrayList<>();
    }

    /**
     * Creates a new event.
     * @param startReg The start date for registration.
     * @param endReg The end date for registration.
     * @param startEvent The start date of the event.
     * @param endEvent The end date of the event.
     * @param name The name of the event.
     * @param description A description of the event.
     * @param location The location of the event.
     * @param radius The radius of the event's location.
     * @param maxEntrants The maximum number of entrants for the event.
     */
    public void createEvent(Date startReg, Date endReg, Date startEvent, Date endEvent, String name, String description, Location location, Integer radius, Integer maxEntrants) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Sends a notification to a list of entrants for a specific event.
     * @param entrants The list of entrants to notify.
     * @param event The event for which to send a notification.
     */
    public void sendNotification(List<Entrant> entrants, Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
