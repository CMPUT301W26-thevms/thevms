package com.example.thevms;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Dedicated test suite for the "Second Chance" functionality.
 * Runs against the Firebase Emulator.
 */
@RunWith(AndroidJUnit4.class)
public class SecondChanceLogicTest {

    private FirestoreTestHelper helper;
    private DatabaseHandler dbHandler;
    private FirebaseFirestore db;

    private static final String TEST_EVENT_ID = "test_event_second_chance";

    @BeforeClass
    public static void checkEmulator() {
        try {
            new java.net.Socket("10.0.2.2", 8080).close();
        } catch (Exception e) {
            throw new RuntimeException("Emulator must be running on 10.0.2.2:8080");
        }
    }

    @Before
    public void setUp() throws Exception {
        helper = new FirestoreTestHelper();
        dbHandler = helper.getDbHandler();
        db = dbHandler.getDb();
        helper.clearDatabase();
    }

    @After
    public void tearDown() throws Exception {
        helper.clearDatabase();
    }

    private void seedEntrant(String userId, String status) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", userId);
        data.put("status", status);
        data.put("registrationTime", new Date());

        Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(TEST_EVENT_ID)
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(userId).set(data), 10, TimeUnit.SECONDS);
    }

    /**
     * Test that declining an invitation triggers the selection of a new entrant.
     */
    @Test
    public void testDeclineTriggersSecondChance() throws Exception {
        // Setup: User A is selected, User B is waiting
        seedEntrant("userA", DatabaseHandler.STATUS_SELECTED);
        seedEntrant("userB", DatabaseHandler.STATUS_WAITING);

        // Action: User A declines
        Map<String, Object> declineData = new HashMap<>();
        declineData.put("status", DatabaseHandler.STATUS_DECLINED);
        Tasks.await(dbHandler.updateEntrantStatus(TEST_EVENT_ID, "userA", declineData), 10, TimeUnit.SECONDS);
        
        // Trigger manual second chance call
        Tasks.await(dbHandler.selectAndInviteNextEntrant(TEST_EVENT_ID), 10, TimeUnit.SECONDS);

        // Verify: User B should now be selected
        DocumentSnapshot docB = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(TEST_EVENT_ID).collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document("userB").get(), 10, TimeUnit.SECONDS);
        
        assertEquals(DatabaseHandler.STATUS_SELECTED, docB.getString("status"));
    }

    /**
     * Test that deleting a selected user's account triggers a second chance.
     */
    @Test
    public void testUserDeletionTriggersSecondChance() throws Exception {
        // Setup: User A is accepted (takes a spot), User B is waiting
        seedEntrant("userA", DatabaseHandler.STATUS_ACCEPTED);
        seedEntrant("userB", DatabaseHandler.STATUS_WAITING);

        // Action: Completely delete User A's account (cascaded deletion)
        Tasks.await(dbHandler.deleteUserAccountCompletely("userA"), 15, TimeUnit.SECONDS);

        // Verify: User B should have been automatically promoted
        DocumentSnapshot docB = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(TEST_EVENT_ID).collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document("userB").get(), 10, TimeUnit.SECONDS);

        assertEquals(DatabaseHandler.STATUS_SELECTED, docB.getString("status"));
    }

    /**
     * Test that second chance doesn't run if no one is waiting.
     */
    @Test
    public void testSecondChanceHandlesEmptyWaitingList() throws Exception {
        // Setup: User A is selected, but NO ONE else is waiting
        seedEntrant("userA", DatabaseHandler.STATUS_SELECTED);

        // Important Fix: First make User A "Declined" to simulate a vacated spot
        Map<String, Object> declineData = new HashMap<>();
        declineData.put("status", DatabaseHandler.STATUS_DECLINED);
        Tasks.await(dbHandler.updateEntrantStatus(TEST_EVENT_ID, "userA", declineData), 10, TimeUnit.SECONDS);

        // Action: Trigger second chance
        Tasks.await(dbHandler.selectAndInviteNextEntrant(TEST_EVENT_ID), 10, TimeUnit.SECONDS);

        // Verify: The selected count should be 0 because the waiting list was empty
        QuerySnapshot selected = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(TEST_EVENT_ID).collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .whereEqualTo("status", DatabaseHandler.STATUS_SELECTED).get(), 10, TimeUnit.SECONDS);

        assertTrue("Should be no selected users because waiting list was empty", selected.isEmpty());
    }
}
