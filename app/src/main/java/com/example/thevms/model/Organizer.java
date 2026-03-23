package com.example.thevms.model;

import android.location.Location;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents an event organizer.
 * Organizers have the ability to create events and manage their entrants.
 */
public class Organizer extends Entrant {
    List<Event> currentEvents;

    /**
     * Constructor for the Organizer class with default notification settings.
     *
     * @param deviceId    The organizer's unique device ID.
     * @param email       The organizer's email address.
     * @param firstName   The organizer's first name.
     * @param lastName    The organizer's last name.
     * @param phoneNumber The organizer's phone number (optional).
     */
    public Organizer(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber) {
        this(deviceId, email, firstName, lastName, phoneNumber, true);
    }

    /**
     * Constructor for the Organizer class with specified notification settings.
     *
     * @param deviceId             The organizer's unique device ID.
     * @param email                The organizer's email address.
     * @param firstName            The organizer's first name.
     * @param lastName             The organizer's last name.
     * @param phoneNumber          The organizer's phone number (optional).
     * @param notificationsEnabled Whether notifications are enabled for this organizer.
     */
    public Organizer(String deviceId, String email, String firstName, String lastName, @Nullable String phoneNumber, boolean notificationsEnabled) {
        super(deviceId, email, firstName, lastName, phoneNumber, notificationsEnabled, UserRole.ORGANIZER);
        this.currentEvents = new ArrayList<>();
    }

    /**
     * Creates an Organizer object from a Map of data.
     *
     * @param deviceId The unique device ID.
     * @param data     The data map from Firestore.
     * @return A populated Organizer object, or null if data is null.
     */
    public static Organizer fromMap(String deviceId, Map<String, Object> data) {
        if (data == null) return null;
        String email = (String) data.get("email");
        String firstName = (String) data.get("firstName");
        String lastName = (String) data.get("lastName");
        String phoneNumber = (String) data.get("phoneNumber");
        Boolean notifications = (Boolean) data.get("notificationsEnabled");
        return new Organizer(deviceId, email, firstName, lastName, phoneNumber, notifications != null ? notifications : true);
    }

    /**
     * Creates a new event (Skeleton method).
     *
     * @param startReg    The start date for registration.
     * @param endReg      The end date for registration.
     * @param startEvent  The start date of the event.
     * @param endEvent    The end date of the event.
     * @param name        The name of the event.
     * @param description A description of the event.
     * @param location    The location of the event.
     * @param radius      The radius of the event's location (if applicable).
     * @param maxEntrants The maximum number of entrants for the event.
     * @throws UnsupportedOperationException if called before implementation.
     */
    public void createEvent(Date startReg, Date endReg, Date startEvent, Date endEvent, String name, String description, Location location, Integer radius, Integer maxEntrants) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Sends a notification to a list of entrants for a specific event (Skeleton method).
     *
     * @param entrants The list of entrants to notify.
     * @param event    The event for which to send a notification.
     * @throws UnsupportedOperationException if called before implementation.
     */
    public void sendNotification(List<Entrant> entrants, Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
