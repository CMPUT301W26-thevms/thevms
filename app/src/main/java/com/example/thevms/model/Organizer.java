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

    public Organizer(String email, String username, String phoneNumber) {
        super(email, username, phoneNumber);
        this.currentEvents = new ArrayList<>();
    }

    public void createEvent(Date startReg, Date endReg, Date startEvent, Date endEvent, String name, String description, Location location, Integer radius, Integer maxEntrants) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sendNotification(List<Entrant> entrants, Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
