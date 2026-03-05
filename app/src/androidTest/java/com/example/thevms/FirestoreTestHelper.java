package com.example.thevms;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
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
        String[] collections = {
                DatabaseHandler.COLLECTION_EVENTS,
                DatabaseHandler.COLLECTION_USERS,
                DatabaseHandler.COLLECTION_ENTRANTS
        };

        FirebaseFirestore db = dbHandler.getDb();
        for (String collection : collections) {
            Tasks.await(db.collection(collection).get().continueWith(task -> {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    doc.getReference().delete();
                }
                return null;
            }), 10, TimeUnit.SECONDS);
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

    public DatabaseHandler getDbHandler() {
        return dbHandler;
    }
}
