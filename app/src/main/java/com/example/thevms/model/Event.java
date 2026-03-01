package com.example.thevms.model;

import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an event Object.
 *
 * <p><b>Creation Workflow:</b></p>
 * <ol>
 *     <li>Call the static {@link #create(String, String, Organizer, Location, String, Date, Date, Date, Date)} method.</li>
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
 * Event.create(name, desc, org, loc, imgUrl, date, date, date, date)
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
    private Location location;
    private String qrCode;
    private String imageUrl;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date eventStartTime;
    private Date eventEndTime;
    private HashMap<Entrant, Boolean> entrantList;

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
     * @return A {@code Task<Event>} that, upon completion, will contain the fully initialized Event object.
     */
    public static Task<Event> create(String name, String description, Organizer organizer, Location location, String imageUrl, Date registrationStartTime, Date registrationEndTime, Date eventStartTime, Date eventEndTime) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        return dbHandler.getNextEventId().continueWith(task -> {
            if (task.isSuccessful()) {
                Long eventId = task.getResult();
                return new Event(eventId, name, description, organizer, location, imageUrl, registrationStartTime, registrationEndTime, eventStartTime, eventEndTime);
            } else {
                // If getting the ID fails, the exception is propagated in the returned Task.
                throw task.getException();
            }
        });
    }

    /**
     * Helper method to safely convert a Firestore object to a Date.
     * <p>
     * Firestore often returns dates as {@link Timestamp} objects. This method
     * handles the conversion from {@link Timestamp} to {@link Date}, or returns
     * the object if it is already a {@link Date}.
     *
     * @param obj The object to convert (typically from a Firestore document).
     * @return The converted {@link Date} object, or {@code null} if the object is null or of an unsupported type.
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
     * <p>
     * This method extracts event properties from a Map, handling date conversions
     * using the {@link #toDate(Object)} helper. It constructs a new Event instance
     * using the extracted values.
     *
     * @param data A Map containing the key-value pairs of the event data.
     * @return A new {@link Event} object populated with the data from the Map, or {@code null} if the data is null.
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

    /**
     * Saves the current state of this Event object to the database.
     * <p>
     * This method takes all the properties of the current object, converts them into a format
     * suitable for the database (a Map), and sends them to be saved or updated in Firestore.
     * This is an asynchronous network operation.
     *
     * @return A {@code Task<Void>} that represents the asynchronous save operation. You can add listeners
     * to this task to be notified of success or failure.
     */
    public Task<Void> save() {
        return dbHandler.saveEvent(this.eventId, this.toMap());
    }

    /**
     * Converts the Event object into a Map format suitable for Firestore.
     * This is a private helper method used by the {@link #save()} method.
     *
     * @return A Map containing the key-value pairs of the event data.
     */
    private Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("name", name);
        map.put("description", description);
        map.put("Organizers", organizer);
        map.put("location", location);
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

    /**
     * Sets the name of the event.
     *
     * @param name The new name of the event.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the event.
     *
     * @return The description of the event.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the event.
     *
     * @param description The new description of the event.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the organizer of the event.
     *
     * @return The organizer of the event.
     */
    public Organizer getOrganizer() {
        return organizer;
    }

    /**
     * Sets the organizer of the event.
     *
     * @param organizer The new organizer of the event.
     */
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    /**
     * Gets the location of the event.
     *
     * @return The location of the event.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of the event.
     *
     * @param location The new location of the event.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the image URL of the event.
     *
     * @return The image URL of the event.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the image URL of the event.
     *
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
     *
     * @return The registration start time.
     */
    public Date getRegistrationStartTime() {
        return registrationStartTime;
    }

    /**
     * Sets the registration start time.
     *
     * @param registrationStartTime The new registration start time.
     */
    public void setRegistrationStartTime(Date registrationStartTime) {
        this.registrationStartTime = registrationStartTime;
    }

    /**
     * Gets the registration end time.
     *
     * @return The registration end time.
     */
    public Date getRegistrationEndTime() {
        return registrationEndTime;
    }

    /**
     * Sets the registration end time.
     *
     * @param registrationEndTime The new registration end time.
     */
    public void setRegistrationEndTime(Date registrationEndTime) {
        this.registrationEndTime = registrationEndTime;
    }

    /**
     * Gets the event start time.
     *
     * @return The event start time.
     */
    public Date getEventStartTime() {
        return eventStartTime;
    }

    /**
     * Sets the event start time.
     *
     * @param eventStartTime The new event start time.
     */
    public void setEventStartTime(Date eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    /**
     * Gets the event end time.
     *
     * @return The event end time.
     */
    public Date getEventEndTime() {
        return eventEndTime;
    }

    /**
     * Sets the event end time.
     *
     * @param eventEndTime The new event end time.
     */
    public void setEventEndTime(Date eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    /**
     * Gets the list of entrants for the event.
     *
     * @return A HashMap of entrants and their selection status.
     */
    public HashMap<Entrant, Boolean> getEntrantList() {
        return entrantList;
    }

    /**
     * Adds an entrant to the event's entrant list.
     *
     * @param entrant    The entrant to add.
     * @param isSelected The selection status of the entrant.
     */
    public void addEntrant(Entrant entrant, Boolean isSelected) {
        this.entrantList.put(entrant, isSelected);
    }

    /**
     * Removes an entrant from the event's entrant list.
     *
     * @param entrant The entrant to remove.
     */
    public void removeEntrant(Entrant entrant) {
        this.entrantList.remove(entrant);
    }

    /**
     * Updates the selection status of an entrant in the event's entrant list.
     *
     * @param entrant
     * @param isSelected
     */
    public void updateEntrant(Entrant entrant, Boolean isSelected) {
        this.entrantList.put(entrant, isSelected);
    }
}
