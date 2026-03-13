package com.example.thevms.model;

import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

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
    private String locationName; // Added for UI convenience

    private String qrCode;
    private String imageUrl;

    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date eventStartTime;
    private Date eventEndTime;
    
    private Integer maxAttendees; // US 01.01.01
    private boolean geolocationRequired; // US 01.08.01
    private Double limitDistance;

    private HashMap<Entrant, Boolean> entrantList;
    private long entrantCount = 0;

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

    private static Date toDate(Object obj) {
        if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toDate();
        } else if (obj instanceof Date) {
            return (Date) obj;
        }
        return null;
    }

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

        Event event = new Event(id, name, desc, null, null, img, regStart, regEnd, eventStart, eventEnd);
        
        if (data.containsKey("maxAttendees") && data.get("maxAttendees") != null) {
            event.setMaxAttendees(((Long) data.get("maxAttendees")).intValue());
        }
        if (data.containsKey("geolocationRequired") && data.get("geolocationRequired") != null) {
            event.setGeolocationRequired((Boolean) data.get("geolocationRequired"));
        }
        if (data.containsKey("limitDistance") && data.get("limitDistance") != null) {
            Object dist = data.get("limitDistance");
            if (dist instanceof Double) event.setLimitDistance((Double) dist);
            else if (dist instanceof Long) event.setLimitDistance(((Long) dist).doubleValue());
        }
        if (data.containsKey("locationName")) {
            event.setLocationName((String) data.get("locationName"));
        }
        if (data.containsKey("organizerId")) {
            event.setOrganizer(new Organizer((String) data.get("organizerId"), null, null, null, null));
        }

        return event;
    }

    public static Event fromDoc(DocumentSnapshot doc) {
        return fromMap(doc.getData());
    }

    public Task<Void> save() {
        return dbHandler.saveEvent(this.eventId, this.toMap());
    }

    private Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("name", name);
        map.put("description", description);
        if (organizer != null) map.put("organizerId", organizer.getDeviceId());
        map.put("location", location);
        map.put("locationName", locationName);
        map.put("imageUrl", imageUrl);
        map.put("registrationStartTime", registrationStartTime);
        map.put("registrationEndTime", registrationEndTime);
        map.put("eventStartTime", eventStartTime);
        map.put("eventEndTime", eventEndTime);
        map.put("maxAttendees", maxAttendees);
        map.put("geolocationRequired", geolocationRequired);
        map.put("limitDistance", limitDistance);
        return map;
    }

    public Task<Boolean> inEvent(Entrant entrant) {
        return dbHandler.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(this.eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(entrant.getDeviceId())
                .get()
                .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }

    public Task<Void> addEntrant(Entrant entrant) {
        Date now = new Date();
        if (registrationStartTime != null && now.before(registrationStartTime)) {
            return Tasks.forException(new IllegalStateException("Registration has not started yet."));
        }
        if (registrationEndTime != null && now.after(registrationEndTime)) {
            return Tasks.forException(new IllegalStateException("Registration has ended."));
        }

        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("entrantId", entrant.getDeviceId());
        registrationData.put("status", DatabaseHandler.STATUS_WAITING);
        registrationData.put("registrationTime", now);

        return dbHandler.updateEntrantStatus(String.valueOf(this.eventId), entrant.getDeviceId(), registrationData);
    }

    public Task<Void> removeEntrant(Entrant entrant) {
        return dbHandler.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(this.eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(entrant.getDeviceId())
                .delete();
    }

    public Task<Long> fetchEntrantCount() {
        return dbHandler.getEntrantCount(String.valueOf(this.eventId))
                .addOnSuccessListener(count -> this.entrantCount = count);
    }

    // Getters and Setters
    public long getEntrantCount() { return entrantCount; }
    public Long getEventId() { return eventId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Organizer getOrganizer() { return organizer; }
    public void setOrganizer(Organizer organizer) { this.organizer = organizer; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Date getRegistrationStartTime() { return registrationStartTime; }
    public void setRegistrationStartTime(Date registrationStartTime) { this.registrationStartTime = registrationStartTime; }
    public Date getRegistrationEndTime() { return registrationEndTime; }
    public void setRegistrationEndTime(Date registrationEndTime) { this.registrationEndTime = registrationEndTime; }
    public Date getEventStartTime() { return eventStartTime; }
    public void setEventStartTime(Date eventStartTime) { this.eventStartTime = eventStartTime; }
    public Date getEventEndTime() { return eventEndTime; }
    public void setEventEndTime(Date eventEndTime) { this.eventEndTime = eventEndTime; }
    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }
    public boolean isGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }
    public Double getLimitDistance() { return limitDistance; }
    public void setLimitDistance(Double limitDistance) { this.limitDistance = limitDistance; }
}
