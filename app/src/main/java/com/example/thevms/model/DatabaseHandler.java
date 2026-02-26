package com.example.thevms.model;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

/**
 * DatabaseHandler provides a clean API for interacting with Firebase Firestore.
 * It manages Events, User Profiles, and Entrant Lists.
 */
public class DatabaseHandler {
    private final FirebaseFirestore db;

    // Collection names
    private static final String COLLECTION_EVENTS = "events";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_ENTRANTS = "entrants";

    public DatabaseHandler() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets the next available event ID.
     *
     * @return A Task that will resolve to the next available event ID.
     */
    public Task<Long> getNextEventId() {
        return db.collection(COLLECTION_EVENTS)
                .orderBy("eventId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            DocumentSnapshot lastEvent = snapshot.getDocuments().get(0);
                            return lastEvent.getLong("eventId") + 1;
                        } else {
                            return 1L; // First event
                        }
                    } else {
                        throw task.getException();
                    }
                });
    }

    /**
     * Stores or updates an event's details.
     *
     * @param eventId   Unique identifier for the event.
     * @param eventData Map containing event attributes.
     * @return Task representing the async operation.
     */
    public Task<Void> saveEvent(Long eventId, Map<String, Object> eventData) {
        return db.collection(COLLECTION_EVENTS).document(String.valueOf(eventId)).set(eventData, SetOptions.merge());
    }

    /**
     * Fetches all events from the database.
     *
     * @return Task containing QuerySnapshot of all events.
     */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection(COLLECTION_EVENTS).get();
    }

    /**
     * Listens for real-time changes to a specific event document.
     *
     * @param eventId  The event ID to listen to.
     * @param listener Callback to handle updates.
     * @return ListenerRegistration to stop listening when needed.
     */
    public ListenerRegistration listenToEvent(String eventId, EventListener<DocumentSnapshot> listener) {
        return db.collection(COLLECTION_EVENTS).document(eventId).addSnapshotListener(listener);
    }

    /**
     * Stores or updates a user profile.
     *
     * @param userId   Unique identifier for the user.
     * @param userData Map containing user profile attributes.
     * @return Task representing the async operation.
     */
    public Task<Void> saveUser(String userId, Map<String, Object> userData) {
        return db.collection(COLLECTION_USERS).document(userId).set(userData, SetOptions.merge());
    }

    /**
     * Retrieves a specific user's profile.
     *
     * @param userId The user ID.
     * @return Task containing DocumentSnapshot of the user.
     */
    public Task<DocumentSnapshot> getUser(String userId) {
        return db.collection(COLLECTION_USERS).document(userId).get();
    }

    /**
     * Updates an entrant's status within an event.
     * Entrants are stored as a sub-collection of an event for scalability.
     *
     * @param eventId    The event ID.
     * @param userId     The user ID of the entrant.
     * @param statusData Map containing status details (e.g., status: "waiting", "selected").
     * @return Task representing the async operation.
     */
    public Task<Void> updateEntrantStatus(String eventId, String userId, Map<String, Object> statusData) {
        return db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .document(userId)
                .set(statusData, SetOptions.merge());
    }

    /**
     * Listens for real-time updates to an entrant's status.
     * Can be used to trigger notifications when an entrant's status changes.
     *
     * @param eventId  The event ID.
     * @param userId   The user ID.
     * @param listener Callback to handle status updates.
     * @return ListenerRegistration.
     */
    public ListenerRegistration listenToEntrantStatus(String eventId, String userId, EventListener<DocumentSnapshot> listener) {
        return db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .document(userId)
                .addSnapshotListener(listener);
    }

    /**
     * Retrieves the list of all entrants for a specific event.
     *
     * @param eventId The event ID.
     * @return Task containing QuerySnapshot of all entrants for the event.
     */
    public Task<QuerySnapshot> getEntrantsForEvent(String eventId) {
        return db.collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .get();
    }

    /**
     * Deletes an event document.
     *
     * @param eventId The event ID to delete.
     * @return Task representing the async operation.
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(COLLECTION_EVENTS).document(eventId).delete();
    }

    public FirebaseFirestore getDb() {
        return db;
    }
}
