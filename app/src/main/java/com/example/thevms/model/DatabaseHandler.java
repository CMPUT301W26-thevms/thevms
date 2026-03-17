package com.example.thevms.model;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DatabaseHandler provides a clean API for interacting with Firebase Firestore.
 * It manages Events, User Profiles, and Entrant Lists.
 */
public class DatabaseHandler {
    private FirebaseFirestore db;

    // Collection names
    public static final String COLLECTION_EVENTS = "events";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_ENTRANTS = "entrants";
    // Entrant status constants
    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_SELECTED = "selected";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_INVITED = "invited";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_CANCELLED = "cancelled";

    /**
     * Default constructor for DatabaseHandler.
     * Lazy initializes the Firestore instance when needed.
     */
    public DatabaseHandler() {
        // Lazy initialization to avoid crashes in unit tests where Firebase is not available
    }

    /**
     * Constructor for testing purposes, allowing injection of a mock or emulator instance.
     *
     * @param db The FirebaseFirestore instance to use.
     */
    public DatabaseHandler(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Configures the handler to use the Firestore emulator.
     * Use this ONLY for local testing.
     *
     * @param host The emulator host (e.g., "10.0.2.2")
     * @param port The emulator port (e.g., 8080)
     */
    public void useEmulator(String host, int port) {
        try {
            getDb().useEmulator(host, port);
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build();
            getDb().setFirestoreSettings(settings);
        } catch (IllegalStateException e) {
            // Already initialized or emulator already set, which is fine for tests
        }
    }

    /**
     * Gets the next available event ID by querying the highest existing ID.
     *
     * @return A Task that will resolve to the next available event ID.
     */
    public Task<Long> getNextEventId() {
        return getDb().collection(COLLECTION_EVENTS)
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
     * Stores or updates an event's details in Firestore.
     *
     * @param eventId   Unique identifier for the event.
     * @param eventData Map containing event attributes.
     * @return Task representing the async operation.
     */
    public Task<Void> saveEvent(Long eventId, Map<String, Object> eventData) {
        return getDb().collection(COLLECTION_EVENTS).document(String.valueOf(eventId)).set(eventData, SetOptions.merge());
    }

    /**
     * Fetches all events from the database.
     *
     * @return Task containing QuerySnapshot of all events.
     */
    public Task<QuerySnapshot> getAllEvents() {
        return getDb().collection(COLLECTION_EVENTS).get();
    }

    /**
     * Fetches events created by a specific organizer.
     *
     * @param organizerId The device ID of the organizer.
     * @return Task containing QuerySnapshot of the organizer's events.
     */
    public Task<QuerySnapshot> getEventsByOrganizer(String organizerId) {
        return getDb().collection(COLLECTION_EVENTS)
                .whereEqualTo("organizerId", organizerId)
                .get();
    }

    /**
     * Listens for real-time changes to a specific event document.
     *
     * @param eventId  The event ID to listen to.
     * @param listener Callback to handle updates.
     * @return ListenerRegistration to stop listening when needed.
     */
    public ListenerRegistration listenToEvent(String eventId, EventListener<DocumentSnapshot> listener) {
        return getDb().collection(COLLECTION_EVENTS).document(eventId).addSnapshotListener(listener);
    }

    /**
     * Stores or updates a user profile in Firestore.
     *
     * @param userId   Unique identifier for the user (device ID).
     * @param userData Map containing user profile attributes.
     * @return Task representing the async operation.
     */
    public Task<Void> saveUser(String userId, Map<String, Object> userData) {
        return getDb().collection(COLLECTION_USERS).document(userId).set(userData, SetOptions.merge());
    }

    /**
     * Retrieves a specific user's profile from Firestore.
     *
     * @param userId The user ID (device ID).
     * @return Task containing DocumentSnapshot of the user.
     */
    public Task<DocumentSnapshot> getUser(String userId) {
        return getDb().collection(COLLECTION_USERS).document(userId).get();
    }

    /**
     * Retrieves all user profiles from the database.
     *
     * @return Task containing QuerySnapshot of all users.
     */
    public Task<QuerySnapshot> getAllUsers() {
        return getDb().collection(COLLECTION_USERS).get();
    }

    /**
     * Performs a comprehensive, cascaded deletion of a user and all their associated data.
     * note - Google Gemini was used to write the function and documentation
     * Deleting a user profile triggers a chain reaction:
     * 1. SCRUBBING REGISTRATIONS: It uses a 'collectionGroup' query to find the user's presence
     * in EVERY event's 'entrants' sub-collection and removes them. This prevents "ghost"
     * entries in event waitlists.
     * 2. REMOVING ORGANIZED EVENTS: It identifies every event where this user is the 'organizerId'
     * and deletes those events entirely. Note: This could potentially affect other users
     * who were registered for those events.
     * 3. FINAL PROFILE DELETE: Only after all dependencies are cleared is the primary
     * user document in the 'users' collection removed.
     * <p>
     * This layered approach ensures database integrity and prevents orphaned data.
     *
     * @param userId The unique device ID of the user to be permanently removed.
     * @return A Task that resolves only when all three stages of the deletion are complete.
     */
    public Task<Void> deleteUserAccountCompletely(String userId) {
        // STAGE 1: Find and delete all event registration records for this user
        // We use collectionGroup because 'entrants' are nested under individual 'events' documents
        return getDb().collectionGroup(COLLECTION_ENTRANTS)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<Task<Void>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        // The document ID in the entrants sub-collection is the user's deviceId
                        if (doc.getId().equals(userId)) {
                            tasks.add(doc.getReference().delete());
                        }
                    }
                    // Wait for all sub-collection deletions to finish
                    return Tasks.whenAll(tasks);
                })
                .continueWithTask(task -> {
                    // STAGE 2: Delete all events organized by this specific user
                    return getDb().collection(COLLECTION_EVENTS)
                            .whereEqualTo("organizerId", userId)
                            .get();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<Task<Void>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        tasks.add(doc.getReference().delete());
                    }
                    // Wait for all organized events to be deleted
                    return Tasks.whenAll(tasks);
                })
                .continueWithTask(task -> {
                    // STAGE 3: Finally, delete the actual user profile document
                    return getDb().collection(COLLECTION_USERS).document(userId).delete();
                });
    }

    /**
     * Deletes a user profile from the database.
     *
     * @param userId The user ID to delete.
     * @return Task representing the async operation.
     */
    public Task<Void> deleteUser(String userId) {
        return getDb().collection(COLLECTION_USERS).document(userId).delete();
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
        return getDb().collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .document(userId)
                .set(statusData, SetOptions.merge());
    }

    /**
     * Retrieves an entrant's current status for a specific event.
     *
     * @param eventId The event ID.
     * @param userId  The user ID.
     * @return A Task that resolves with the status string.
     */
    public Task<String> getEntrantStatus(String eventId, String userId) {
        return getDb().collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("status");
                    }
                    return null;
                });
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
        return getDb().collection(COLLECTION_EVENTS)
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
        return getDb().collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .get();
    }

    /**
     * Retrieves all registration documents for a specific entrant across all events.
     *
     * @param entrantId The unique device ID of the entrant.
     * @return A Task containing the QuerySnapshot of registrations.
     */
    public Task<QuerySnapshot> getRegistrationsForEntrant(String entrantId) {
        return getDb().collectionGroup(COLLECTION_ENTRANTS)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Deletes an event document along with all its nested entrants.
     *
     * @param eventId The event ID to delete.
     * @return Task representing the async operation.
     */
    public Task<Void> deleteEvent(String eventId) {
        return getDb().collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    WriteBatch batch = getDb().batch();

                    // Delete all entrant documents in the sub-collection
                    if (task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            batch.delete(doc.getReference());
                        }
                    }

                    // Delete the event document itself
                    batch.delete(getDb().collection(COLLECTION_EVENTS).document(eventId));

                    return batch.commit();
                });
    }

    /**
     * Retrieves the count of entrants for a specific event.
     *
     * @param eventId The event ID.
     * @return A Task that resolves with the count of entrants.
     */
    public Task<Long> getEntrantCount(String eventId) {
        return getDb().collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return (long) task.getResult().size();
                    } else {
                        throw task.getException();
                    }
                });
    }

    /**
     * Randomly selects one waiting entrant for an event and invites them.
     *
     * @param eventId The event ID.
     * @return A Task representing the operation.
     */
    public Task<Void> selectAndInviteNextEntrant(String eventId) {
        return getDb().collection(COLLECTION_EVENTS)
                .document(eventId)
                .collection(COLLECTION_ENTRANTS)
                .whereEqualTo("status", STATUS_WAITING)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    List<DocumentSnapshot> waiting = task.getResult().getDocuments();
                    DocumentSnapshot next = waiting.get(new Random().nextInt(waiting.size()));

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", STATUS_SELECTED);
                    return updateEntrantStatus(eventId, next.getId(), data);
                });
    }

    /**
     * Gets the current Firestore instance, initializing it if necessary.
     *
     * @return The FirebaseFirestore instance.
     */
    public FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
