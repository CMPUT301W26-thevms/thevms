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
 *
 * <p><b>Creation Workflow:</b></p>
 * <ol>
 *     <li>Call the static {@link #create(String, String, Organizer, String, String, Date, Date, Date, Date, boolean, Double, Location)} method.</li>
 *     <li>This method asynchronously fetches a unique ID from the database.</li>
 *     <li>It then returns a {@code Task<Event>}. You use an {@code .addOnSuccessListener} to get the in-memory Event object.</li>
 *     <li>The created object does NOT yet exist in the database.</li>
 * </ol>
 *
 * <p><b>Saving and Updating:</b></p>
 * <ul>
 *     <li>To save the event to the database for the first time, or to update it after making changes, you MUST call the {@link #save()} method.</li>
 *     <li>The {@code save()} method is also asynchronous and returns a {@code Task<Void>} that you can listen to for success or failure.</li>
 * </ul>
 * <p>
 * Example Usage:
 * <pre>
 * {@code
 * // 1. Create the event in memory (gets a unique ID async)
 * Event.create(name, desc, org, loc, imgUrl, date, date, date, date, geolocationRequired, radius, geoLocation)
 *      .addOnSuccessListener(event -> {
 *          // 2. Save the event to the database
 *          event.save()
 *               .addOnSuccessListener(aVoid -> {
 *                   // Success!
 *               });
 *               .addOnFailureListener(e -> {
 *                   // Error!
 *               });
 *      });
 * }
 * </pre>
 */
public class Event {
    private final DatabaseHandler dbHandler;
    private Long eventId;
    private String name;
    private String description;
    private Organizer organizer;
    private String location;
    private Location geoLocation; // location coordinate
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
    private Double radius = 0.0;

    /**
     * Constructor for the Event class.
     *
     * @param eventId               The unique ID fetched from the database.
     * @param name                  Name of the event.
     * @param description           Description of the event.
     * @param organizer             The event organizer.
     * @param location              The event location.
     * @param imageUrl              URL for the event's banner image.
     * @param registrationStartTime The start time for registration.
     * @param registrationEndTime   The end time for registration.
     * @param eventStartTime        The start time of the event.
     * @param eventEndTime          The end time of the event.
     * @param geolocationRequired   Whether joining requires being within a certain distance.
     * @param radius                The allowed distance radius (in kilometers).
     * @param geoLocation           The actual GPS coordinates (Latitude/Longitude) of the event.
     */
    private Event(Long eventId, String name, String description, Organizer organizer, String location, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime, boolean geolocationRequired, Double radius, Location geoLocation) {
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

        this.geolocationRequired = geolocationRequired;
        this.radius = radius;
        this.geoLocation = geoLocation;
    }

    /**
     * This method was written using - Google Gemini
     * Asynchronously creates an Event object in memory with a unique ID from the database.
     * <p>
     * This method is the designated factory for creating new Event instances.
     * It first makes a network call to the database to secure a new, unique eventId.
     * Only after the ID is successfully retrieved does it construct the Event object in memory.
     * <p>
     * <b>Important:</b> This method only creates the object locally. It does NOT save it to the database.
     * You must call the {@link #save()} method on the returned object to persist it.
     *
     * @param name                  Name of the event.
     * @param description           Description of the event.
     * @param organizer             The event organizer.
     * @param location              The event location.
     * @param imageUrl              URL for the event's banner image.
     * @param registrationStartTime The start time for registration.
     * @param registrationEndTime   The end time for registration.
     * @param eventStartTime        The start time of the event.
     * @param eventEndTime          The end time of the event.
     * @param geolocationRequired   Boolean flag for geofencing.
     * @param radius                The radius limit for joining (in km).
     * @param geoLocation           The {@link Location} object containing coordinates.
     * @return A {@code Task<Event>} that, upon completion, will contain the fully initialized Event object.
     */
    public static Task<Event> create(String name, String description, Organizer organizer, String location, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime, boolean geolocationRequired, Double radius, Location geoLocation) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        return dbHandler.getNextEventId().continueWith(task -> {
            if (task.isSuccessful()) {
                Long eventId = task.getResult();
                return new Event(eventId, name, description, organizer, location, imageUrl, registrationStartTime, registrationEndTime, eventStartTime, eventEndTime, geolocationRequired, radius, geoLocation);
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

        Long id = data.get("eventId") instanceof Long ? (Long) data.get("eventId") : null;
        String name = (String) data.get("name");
        String desc = (String) data.get("description");
        String img = (String) data.get("imageUrl");
        String orgId = (String) data.get("organizerId");
        String email = (String) data.get("organizerEmail");
        String fName = (String) data.get("organizerFirstName");
        String lName = (String) data.get("organizerLastName");
        String phone = (String) data.get("organizerPhone");
        Organizer organizer = (orgId != null) ? new Organizer(orgId, email, fName, lName, phone) : null;
        String location = (String) data.get("location");
        Date regStart = toDate(data.get("registrationStartTime"));
        Date regEnd = toDate(data.get("registrationEndTime"));
        Date eventStart = toDate(data.get("eventStartTime"));
        Date eventEnd = toDate(data.get("eventEndTime"));
        Boolean geoRequired = data.get("geolocationRequired") instanceof Boolean ? (Boolean) data.get("geolocationRequired") : false;
        Double radius = data.get("radius") instanceof Double ? (Double) data.get("radius") : 0.0;
        
        Location geoLocation = null;
        if (data.containsKey("latitude") && data.get("latitude") != null && data.containsKey("longitude") && data.get("longitude") != null) {
            geoLocation = new Location("event_location");
            geoLocation.setLatitude((Double) data.get("latitude"));
            geoLocation.setLongitude((Double) data.get("longitude"));
        }

        Event event = new Event(id, name, desc, organizer, location, img, regStart, regEnd, eventStart, eventEnd, geoRequired != null && geoRequired, radius, geoLocation);

        if (data.containsKey("maxAttendees") && data.get("maxAttendees") != null) {
            event.setMaxAttendees(((Long) data.get("maxAttendees")).intValue());
        }
        if (data.containsKey("limitDistance") && data.get("limitDistance") != null) {
            Object dist = data.get("limitDistance");
            if (dist instanceof Double) event.setLimitDistance((Double) dist);
            else if (dist instanceof Long) event.setLimitDistance(((Long) dist).doubleValue());
        }
        if (data.containsKey("locationName")) {
            event.setLocationName((String) data.get("locationName"));
        }

        return event;
    }

    public static Event fromDoc(DocumentSnapshot doc) {
        if (!doc.exists()) return null;
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
        if (organizer != null) {
            map.put("organizerId", organizer.getDeviceId());
            map.put("organizerEmail", organizer.getEmail());
            map.put("organizerFirstName", organizer.getFirstName());
            map.put("organizerLastName", organizer.getLastName());
            map.put("organizerPhone", organizer.getPhoneNumber());
        }
        map.put("location", location);
        map.put("locationName", locationName);
        map.put("imageUrl", imageUrl);
        map.put("registrationStartTime", registrationStartTime);
        map.put("registrationEndTime", registrationEndTime);
        map.put("eventStartTime", eventStartTime);
        map.put("eventEndTime", eventEndTime);
        map.put("geolocationRequired", geolocationRequired);
        map.put("radius", radius);
        if (geoLocation != null) {
            map.put("latitude", geoLocation.getLatitude());
            map.put("longitude", geoLocation.getLongitude());
        }
        map.put("maxAttendees", maxAttendees);
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


    public long getEntrantCount() {return entrantCount;}
    public Long getEventId() {return eventId;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public Organizer getOrganizer() {return organizer;}
    public void setOrganizer(Organizer organizer) {this.organizer = organizer;}
    public String getLocation() {return location;}
    public void setLocation(String location) {this.location = location;}
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getImageUrl() {return imageUrl;}
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl;}
    public void removeImageUrl() {this.imageUrl = null;}
    public Date getRegistrationStartTime() {return registrationStartTime;}
    public void setRegistrationStartTime(Date registrationStartTime) {this.registrationStartTime = registrationStartTime;}
    public Date getRegistrationEndTime() {return registrationEndTime;}
    public void setRegistrationEndTime(Date registrationEndTime) {this.registrationEndTime = registrationEndTime;}
    public Date getEventStartTime() {return eventStartTime;}
    public void setEventStartTime(Date eventStartTime) {this.eventStartTime = eventStartTime;}
    public Date getEventEndTime() {return eventEndTime;}
    public void setEventEndTime(Date eventEndTime) {this.eventEndTime = eventEndTime;}
    public HashMap<Entrant, Boolean> getEntrantList() {return entrantList;}
    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }
    public boolean isGeolocationRequired() {return geolocationRequired;}
    public void setGeolocationRequired(boolean geolocationRequired) {this.geolocationRequired = geolocationRequired;}
    public Double getRadius() {return radius;}
    public void setRadius(Double radius) {this.radius = radius;}
    public Location getGeoLocation() {return geoLocation;}
    public void setGeoLocation(Location geoLocation) {this.geoLocation = geoLocation;}
    public Double getLimitDistance() { return limitDistance; }
    public void setLimitDistance(Double limitDistance) { this.limitDistance = limitDistance; }
}
