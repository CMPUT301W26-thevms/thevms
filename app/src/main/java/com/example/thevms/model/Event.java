package com.example.thevms.model;

import android.location.Location;

import java.util.Date;
import java.util.HashMap;

/**
 * Represents an event.
 */
public class Event {
    private String name;
    private String description;
    private Organizer organizer;
    private Location location;

    private String qrCode;
    private String imageUrl;

    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date eventStartTime;
    private Date eventEndTime;
    //  ASK if events can for example "happen every monday"  private Boolean isReoccurring;

    private HashMap<Entrant, Boolean> entrantList;

    public Event(String name, String description, Organizer organizer, Location location, String qrCode, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime) {
        this.name = name;
        this.description = description;
        this.organizer = organizer;
        this.location = location;
        this.qrCode = qrCode;
        this.imageUrl = imageUrl;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.eventStartTime = eventStartTime;
        this.eventEndTime = eventEndTime;
        this.entrantList = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void removeImageUrl() {
        this.imageUrl = null;
    }

    public Date getRegistrationStartTime() {
        return registrationStartTime;
    }

    public void setRegistrationStartTime(Date registrationStartTime) {
        this.registrationStartTime = registrationStartTime;
    }

    public Date getRegistrationEndTime() {
        return registrationEndTime;
    }

    public void setRegistrationEndTime(Date registrationEndTime) {
        this.registrationEndTime = registrationEndTime;
    }

    public Date getEventStartTime() {
        return eventStartTime;
    }

    public void setEventStartTime(Date eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public Date getEventEndTime() {
        return eventEndTime;
    }

    public void setEventEndTime(Date eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    public HashMap<Entrant, Boolean> getEntrantList() {
        return entrantList;
    }

    public void addEntrant(Entrant entrant, Boolean isSelected) {
        this.entrantList.put(entrant, isSelected);
    }

    public void removeEntrant(Entrant entrant) {
        this.entrantList.remove(entrant);
    }
}
