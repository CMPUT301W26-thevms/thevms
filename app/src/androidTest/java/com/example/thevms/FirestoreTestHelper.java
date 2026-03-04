package com.example.thevms;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
     * Prepopulates the database with dummy events for testing.
     */
    public void seedDummyEvents(int count) {
        for (int i = 1; i <= count; i++) {
            Event.create(
                    "Test Event " + i,
                    "Description for event " + i,
                    null, // Organizer
                    null, // Location
                    null, // Image URL
                    new Date(), // Reg Start
                    new Date(System.currentTimeMillis() + 86400000), // Reg End
                    new Date(System.currentTimeMillis() + 172800000), // Event Start
                    new Date(System.currentTimeMillis() + 259200000)  // Event End
            ).addOnSuccessListener(Event::save);
        }
    }

    public DatabaseHandler getDbHandler() {
        return dbHandler;
    }
}
