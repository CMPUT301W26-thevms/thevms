package com.example.thevms;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for managing a test database state.
 * Designed for use in Instrumented (UI) and Unit tests.
 */
public class FirestoreTestHelper {

    private final DatabaseHandler dbHandler;

    public FirestoreTestHelper() {
        this.dbHandler = new DatabaseHandler();
        // Point to the local emulator.
        // 10.0.2.2 is the special alias for the host machine from the Android emulator.
        this.dbHandler.useEmulator("10.0.2.2", 8080);
    }

    /**
     * Clears all data from the test collections.
     * WARNING: This should only be called on an emulator instance.
     */
    public void clearDatabase() throws ExecutionException, InterruptedException, TimeoutException {
        FirebaseFirestore db = dbHandler.getDb();

        // 1. Clear top-level collections
        String[] collections = {
                DatabaseHandler.COLLECTION_EVENTS,
                DatabaseHandler.COLLECTION_USERS
        };

        for (String collection : collections) {
            QuerySnapshot snapshot = Tasks.await(db.collection(collection).get(), 10, TimeUnit.SECONDS);
            List<Task<Void>> deleteTasks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                deleteTasks.add(doc.getReference().delete());
            }
            if (!deleteTasks.isEmpty()) {
                Tasks.await(Tasks.whenAll(deleteTasks), 10, TimeUnit.SECONDS);
            }
        }

        // 2. Clear entrants using collectionGroup (since they are sub-collections)
        QuerySnapshot entrantSnapshot = Tasks.await(db.collectionGroup(DatabaseHandler.COLLECTION_ENTRANTS).get(), 10, TimeUnit.SECONDS);
        List<Task<Void>> entrantDeleteTasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : entrantSnapshot) {
            entrantDeleteTasks.add(doc.getReference().delete());
        }
        if (!entrantDeleteTasks.isEmpty()) {
            Tasks.await(Tasks.whenAll(entrantDeleteTasks), 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Seeds events with specific names and dates for filtering tests.
     */
    public void seedTestEvents() throws ExecutionException, InterruptedException, TimeoutException {
        Organizer mockOrganizer = new Organizer("test-device-id", "test@example.com", "Test", "Organizer", null);

        // Event 1: Today, 10:00 AM
        Calendar cal1 = Calendar.getInstance();
        cal1.set(Calendar.HOUR_OF_DAY, 10);
        cal1.set(Calendar.MINUTE, 0);
        seedEvent("Morning Event", cal1.getTime(), mockOrganizer);

        // Event 2: Tomorrow, 2:00 PM (14:00)
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.DAY_OF_YEAR, 1);
        cal2.set(Calendar.HOUR_OF_DAY, 14);
        cal2.set(Calendar.MINUTE, 0);
        seedEvent("Afternoon Event", cal2.getTime(), mockOrganizer);
    }

    private void seedEvent(String name, Date startDate, Organizer organizer) throws ExecutionException, InterruptedException, TimeoutException {
        Event event = Tasks.await(Event.create(
                name,
                "Description",
                organizer,
                null,
                null,
                new Date(), // Reg Start
                new Date(System.currentTimeMillis() + 86400000), // Reg End
                startDate,
                new Date(startDate.getTime() + 3600000) // End +1h
        ), 10, TimeUnit.SECONDS);
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);
    }

    public void seedDummyEvents(int count) throws ExecutionException, InterruptedException, TimeoutException {
        Organizer mockOrganizer = new Organizer("test-device-id", "test@example.com", "Test", "Organizer", null);
        for (int i = 1; i <= count; i++) {
            seedEvent("Test Event " + i, new Date(), mockOrganizer);
        }
    }

    /**
     * Seeds dummy entrants for a specific event to simulate existing participants.
     *
     * @param eventId The ID of the event to seed entrants for.
     * @param count   The number of entrants to seed.
     */
    public void seedEntrants(long eventId, int count) throws ExecutionException, InterruptedException, TimeoutException {
        FirebaseFirestore db = dbHandler.getDb();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("entrantId", "seeded-entrant-" + i);
            data.put("status", "waiting");
            data.put("registrationTime", new Date());

            Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                    .document(String.valueOf(eventId))
                    .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                    .document("seeded-entrant-" + i)
                    .set(data), 10, TimeUnit.SECONDS);
        }
    }

    public DatabaseHandler getDbHandler() {
        return dbHandler;
    }
}
