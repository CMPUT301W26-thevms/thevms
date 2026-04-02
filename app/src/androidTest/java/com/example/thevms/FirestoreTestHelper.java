package com.example.thevms;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.Comment;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    private static final String TAG = "FirestoreTestHelper";

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
                DatabaseHandler.COLLECTION_USERS,
                DatabaseHandler.COLLECTION_NOTIFICATIONS
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

        // 2. Clear entrants and comments using collectionGroup (since they are sub-collections)
        String[] subCollections = {
                DatabaseHandler.COLLECTION_ENTRANTS,
                DatabaseHandler.COLLECTION_COMMENTS
        };

        for (String subColl : subCollections) {
            QuerySnapshot subSnapshot = Tasks.await(db.collectionGroup(subColl).get(), 10, TimeUnit.SECONDS);
            List<Task<Void>> subDeleteTasks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : subSnapshot) {
                subDeleteTasks.add(doc.getReference().delete());
            }
            if (!subDeleteTasks.isEmpty()) {
                Tasks.await(Tasks.whenAll(subDeleteTasks), 10, TimeUnit.SECONDS);
            }
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
                new Date(startDate.getTime() + 3600000), // End +1h
                false,
                0.0,
                null,
                false
        ), 10, TimeUnit.SECONDS);
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);
    }

    public void seedDummyEvents(int count) throws ExecutionException, InterruptedException, TimeoutException {
        Organizer mockOrganizer = new Organizer("test-device-id", "test@example.com", "Test", "Organizer", null);
        for (int i = 1; i <= count; i++) {
            seedEvent("Test Event " + i, new Date(), mockOrganizer);
        }
    }

    public void seedDummyEventsForOrganizer(int count, String organizerId) throws ExecutionException, InterruptedException, TimeoutException {
        Organizer mockOrganizer = new Organizer(organizerId, "test@example.com", "Test", "Organizer", null);
        for (int i = 1; i <= count; i++) {
            seedEvent("Test Event " + i, new Date(), mockOrganizer);
        }
    }

    public void seedComment(String eventId, String userId, String firstName, String lastName, String text) throws ExecutionException, InterruptedException, TimeoutException {
        Comment comment = new Comment(userId, firstName, lastName, text, new Date());
        Tasks.await(dbHandler.addComment(eventId, comment), 10, TimeUnit.SECONDS);
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

    /**
     * Seeds specific entrants for a specific event to simulate existing participants.
     * @param eventId The ID of the event to seed entrants for.
     * @param deviceId The device ID of the entrant.
     */
    public void seedSpecificEntrant(long eventId, String deviceId) throws ExecutionException, InterruptedException, TimeoutException {
        FirebaseFirestore db = dbHandler.getDb();

        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", deviceId);
        data.put("status", "waiting");
        data.put("registrationTime", new Date());

        Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(deviceId)
                .set(data), 10, TimeUnit.SECONDS);
    }


    public void setMockLocation(double lat, double lng) throws Exception {
        LocationManager locationManager = (LocationManager) InstrumentationRegistry.getInstrumentation()
                .getContext().getSystemService(Context.LOCATION_SERVICE);

        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused"};

        for (String provider : providers) {
            try {
                if (locationManager.getProvider(provider) != null) {
                    locationManager.removeTestProvider(provider);
                }
            } catch (Exception ignored) {}

            try {
                locationManager.addTestProvider(provider, false, false, false, false, true, true, true, 1, 1);
                locationManager.setTestProviderEnabled(provider, true);

                Location mockLocation = new Location(provider);
                mockLocation.setLatitude(lat);
                mockLocation.setLongitude(lng);
                mockLocation.setAltitude(0);
                mockLocation.setTime(System.currentTimeMillis());
                mockLocation.setAccuracy(1.0f);
                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                locationManager.setTestProviderLocation(provider, mockLocation);
            } catch (Exception ignored) {}
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(
                InstrumentationRegistry.getInstrumentation().getTargetContext());

        Location mockLocation = new Location("fused");
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lng);
        mockLocation.setAltitude(0);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setAccuracy(1.0f);
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        try {
            Tasks.await(fusedClient.setMockMode(true), 5, TimeUnit.SECONDS);
            Tasks.await(fusedClient.setMockLocation(mockLocation), 5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                Log.w(TAG, "Fused mock location not allowed; falling back to LocationManager test providers", cause);
            } else {
                throw e;
            }
        }

        Thread.sleep(2000);
    }

    public DatabaseHandler getDbHandler() {
        return dbHandler;
    }
}
