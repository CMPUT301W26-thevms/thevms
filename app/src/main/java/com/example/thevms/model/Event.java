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

    /**
     * Constructor for the Event class.
     * @param name The name of the event.
     * @param description A description of the event.
     * @param organizer The organizer of the event.
     * @param location The location of the event.
     * @param imageUrl The URL for the event's image.
     * @param registrationStartTime The start time for registration.
     * @param registrationEndTime The end time for registration.
     * @param eventStartTime The start time of the event.
     * @param eventEndTime The end time of the event.
     */
    public Event(String name, String description, Organizer organizer, Location location, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime) {
        this.name = name;
        this.description = description;
        this.organizer = organizer;
        this.location = location;
        this.imageUrl = imageUrl;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.eventStartTime = eventStartTime;
        this.eventEndTime = eventEndTime;
        this.entrantList = new HashMap<>();

        // Set the QR Code
    }

    /**
     * Gets the name of the event.
     * @return The name of the event.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     * @param name The new name of the event.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the event.
     * @return The description of the event.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the event.
     * @param description The new description of the event.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the organizer of the event.
     * @return The organizer of the event.
     */
    public Organizer getOrganizer() {
        return organizer;
    }

    /**
     * Sets the organizer of the event.
     * @param organizer The new organizer of the event.
     */
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    /**
     * Gets the location of the event.
     * @return The location of the event.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of the event.
     * @param location The new location of the event.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the image URL of the event.
     * @return The image URL of the event.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the image URL of the event.
     * @param imageUrl The new image URL of the event.
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Removes the image URL of the event.
     */
    public void removeImageUrl() {
        this.imageUrl = null;
    }

    /**
     * Gets the registration start time.
     * @return The registration start time.
     */
    public Date getRegistrationStartTime() {
        return registrationStartTime;
    }

    /**
     * Sets the registration start time.
     * @param registrationStartTime The new registration start time.
     */
    public void setRegistrationStartTime(Date registrationStartTime) {
        this.registrationStartTime = registrationStartTime;
    }

    /**
     * Gets the registration end time.
     * @return The registration end time.
     */
    public Date getRegistrationEndTime() {
        return registrationEndTime;
    }

    /**
     * Sets the registration end time.
     * @param registrationEndTime The new registration end time.
     */
    public void setRegistrationEndTime(Date registrationEndTime) {
        this.registrationEndTime = registrationEndTime;
    }

    /**
     * Gets the event start time.
     * @return The event start time.
     */
    public Date getEventStartTime() {
        return eventStartTime;
    }

    /**
     * Sets the event start time.
     * @param eventStartTime The new event start time.
     */
    public void setEventStartTime(Date eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    /**
     * Gets the event end time.
     * @return The event end time.
     */
    public Date getEventEndTime() {
        return eventEndTime;
    }

    /**
     * Sets the event end time.
     * @param eventEndTime The new event end time.
     */
    public void setEventEndTime(Date eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    /**
     * Gets the list of entrants for the event.
     * @return A HashMap of entrants and their selection status.
     */
    public HashMap<Entrant, Boolean> getEntrantList() {
        return entrantList;
    }

    /**
     * Adds an entrant to the event's entrant list.
     * @param entrant The entrant to add.
     * @param isSelected The selection status of the entrant.
     */
    public void addEntrant(Entrant entrant, Boolean isSelected) {
        this.entrantList.put(entrant, isSelected);
    }

    /**
     * Removes an entrant from the event's entrant list.
     * @param entrant The entrant to remove.
     */
    public void removeEntrant(Entrant entrant) {
        this.entrantList.remove(entrant);
    }
}
