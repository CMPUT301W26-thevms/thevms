package com.example.thevms.model;

import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an event Object.
 */
public class Event {
    private final DatabaseHandler dbHandler;
    private Long eventId;
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
    private HashMap<Entrant, Boolean> entrantList;

    /**
     * Internal constructor for the Event class.
     */
    private Event(Long eventId, String name, String description, Organizer organizer, Location location, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime) {
        this.dbHandler = new DatabaseHandler();
        this.eventId = eventId;
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
        this.qrCode = "NULL FOR NOW";
    }

    /**
     * Design-specific factory for creating new Event instances with a unique ID from the database.
     */
    public static Task<Event> create(String name, String description, Organizer organizer, Location location, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        return dbHandler.getNextEventId().continueWith(task -> {
            if (task.isSuccessful()) {
                Long eventId = task.getResult();
                return new Event(eventId, name, description, organizer, location, imageUrl, registrationStartTime, registrationEndTime, eventStartTime, eventEndTime);
            } else {
                throw task.getException();
            }
        });
    }

    /**
     * Helper method to safely convert a Firestore object to a Date.
     */
    private static Date toDate(Object obj) {
        if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toDate();
        } else if (obj instanceof Date) {
            return (Date) obj;
        }
        return null;
    }

    /**
     * Creates an Event object from a Map of data (typically from Firestore).
     */
    public static Event fromMap(Map<String, Object> data) {
        if (data == null) return null;

        Long id = (Long) data.get("eventId");
        String name = (String) data.get("name");
        String desc = (String) data.get("description");
        String img = (String) data.get("imageUrl");
        
        Date regStart = toDate(data.get("registrationStartTime"));
        Date regEnd = toDate(data.get("registrationEndTime"));
        Date eventStart = toDate(data.get("eventStartTime"));
        Date eventEnd = toDate(data.get("eventEndTime"));

        // Location and Organizer might need specialized mapping depending on how they are stored
        // For now, we'll initialize them as null or use placeholders if necessary for the UI task.

        return new Event(id, name, desc, null, null, img, regStart, regEnd, eventStart, eventEnd);
    }

    public Task<Void> save() {
        return dbHandler.saveEvent(this.eventId, this.toMap());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("name", name);
        map.put("description", description);
        map.put("imageUrl", imageUrl);
        map.put("registrationStartTime", registrationStartTime);
        map.put("registrationEndTime", registrationEndTime);
        map.put("eventStartTime", eventStartTime);
        map.put("eventEndTime", eventEndTime);
        return map;
    }

    // Getters and Setters
    public Long getEventId() {
        return eventId;
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getEventStartTime() {
        return eventStartTime;
    }

    public HashMap<Entrant, Boolean> getEntrantList() {
        return entrantList;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }
}
