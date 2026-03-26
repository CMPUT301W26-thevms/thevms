package com.example.thevms;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented Firestore tests for entrant status features.
 *
 * These tests run against the Firebase Local Emulator (10.0.2.2:8080).
 * Make sure the emulator is running before executing these tests.
 *
 * Place in: src/androidTest/java/com/example/thevms/
 *
 * Tests cover:
 *  - Fetching entrants by status from the subcollection
 *  - Cancelling a selected entrant (status → "cancelled")
 *  - Auto-promoting a waiting entrant to "selected" after a cancel
 *  - Edge case: no waiting entrants left after cancel
 *  - Edge case: multiple waiting entrants — only one gets promoted
 */
@RunWith(AndroidJUnit4.class)
public class EntrantStatusFirestoreTest {

    private FirestoreTestHelper helper;
    private DatabaseHandler dbHandler;
    private FirebaseFirestore db;

    // We use a fixed test event ID so we don't need to create a full Event object
    private static final long TEST_EVENT_ID = 99999L;
    private static final String EVENT_DOC_ID = String.valueOf(TEST_EVENT_ID);

    @BeforeClass
    public static void checkEmulator() {
        try {
            new java.net.Socket("10.0.2.2", 8080).close();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Firestore emulator not running on 10.0.2.2:8080. " +
                            "Run: firebase emulators:start --only firestore", e
            );
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

    // ── Helper: seed an entrant with a specific status ────────────────────────

    private void seedEntrantWithStatus(String entrantId, String status) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", entrantId);
        data.put("status", status);
        data.put("registrationTime", new Date());

        Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document(entrantId)
                        .set(data),
                10, TimeUnit.SECONDS
        );
    }

    private DocumentSnapshot getEntrantDoc(String entrantId) throws Exception {
        return Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document(entrantId)
                        .get(),
                10, TimeUnit.SECONDS
        );
    }

    private QuerySnapshot getEntrantsByStatus(String status) throws Exception {
        return Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .whereEqualTo("status", status)
                        .get(),
                10, TimeUnit.SECONDS
        );
    }

    // ── getEntrantsForEvent ───────────────────────────────────────────────────

    @Test
    public void getEntrantsForEvent_returnsAllEntrants() throws Exception {
        helper.seedEntrants(TEST_EVENT_ID, 3);

        QuerySnapshot result = Tasks.await(
                dbHandler.getEntrantsForEvent(EVENT_DOC_ID),
                10, TimeUnit.SECONDS
        );

        assertEquals(3, result.size());
    }

    @Test
    public void getEntrantsForEvent_emptyEvent_returnsZero() throws Exception {
        QuerySnapshot result = Tasks.await(
                dbHandler.getEntrantsForEvent(EVENT_DOC_ID),
                10, TimeUnit.SECONDS
        );

        assertEquals(0, result.size());
    }

    @Test
    public void getEntrantsForEvent_entrantHasCorrectFields() throws Exception {
        helper.seedSpecificEntrant(TEST_EVENT_ID, "device_001");

        QuerySnapshot result = Tasks.await(
                dbHandler.getEntrantsForEvent(EVENT_DOC_ID),
                10, TimeUnit.SECONDS
        );

        assertEquals(1, result.size());

        DocumentSnapshot doc = result.getDocuments().get(0);
        assertEquals("device_001", doc.getString("entrantId"));
        assertEquals("waiting", doc.getString("status"));
        assertNotNull(doc.getDate("registrationTime"));
    }

    // ── Status filtering ──────────────────────────────────────────────────────

    @Test
    public void filterByStatus_waiting_returnsOnlyWaiting() throws Exception {
        seedEntrantWithStatus("e001", "waiting");
        seedEntrantWithStatus("e002", "waiting");
        seedEntrantWithStatus("e003", "selected");
        seedEntrantWithStatus("e004", "accepted");

        QuerySnapshot result = getEntrantsByStatus("waiting");

        assertEquals(2, result.size());
        for (QueryDocumentSnapshot doc : result) {
            assertEquals("waiting", doc.getString("status"));
        }
    }

    @Test
    public void filterByStatus_selected_returnsOnlySelected() throws Exception {
        seedEntrantWithStatus("e001", "waiting");
        seedEntrantWithStatus("e002", "selected");
        seedEntrantWithStatus("e003", "accepted");

        QuerySnapshot result = getEntrantsByStatus("selected");

        assertEquals(1, result.size());
        assertEquals("selected", result.getDocuments().get(0).getString("status"));
    }

    @Test
    public void filterByStatus_noMatches_returnsEmpty() throws Exception {
        seedEntrantWithStatus("e001", "waiting");
        seedEntrantWithStatus("e002", "waiting");

        QuerySnapshot result = getEntrantsByStatus("accepted");

        assertEquals(0, result.size());
    }

    // ── Cancel entrant ────────────────────────────────────────────────────────

    @Test
    public void cancelEntrant_setsStatusToCancelled() throws Exception {
        seedEntrantWithStatus("e001", "selected");

        // Perform the cancel write
        Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document("e001")
                        .update("status", "cancelled"),
                10, TimeUnit.SECONDS
        );

        DocumentSnapshot doc = getEntrantDoc("e001");
        assertEquals("cancelled", doc.getString("status"));
    }

    @Test
    public void cancelEntrant_doesNotAffectOtherEntrants() throws Exception {
        seedEntrantWithStatus("e001", "selected");
        seedEntrantWithStatus("e002", "waiting");
        seedEntrantWithStatus("e003", "accepted");

        Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document("e001")
                        .update("status", "cancelled"),
                10, TimeUnit.SECONDS
        );

        assertEquals("waiting",  getEntrantDoc("e002").getString("status"));
        assertEquals("accepted", getEntrantDoc("e003").getString("status"));
    }

    // ── Auto-select next waiting entrant ──────────────────────────────────────

    @Test
    public void afterCancel_oneWaitingEntrant_getsPromotedToSelected() throws Exception {
        seedEntrantWithStatus("e001", "selected");
        seedEntrantWithStatus("e002", "waiting");

        // Step 1: cancel e001
        Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document("e001")
                        .update("status", "cancelled"),
                10, TimeUnit.SECONDS
        );

        // Step 2: fetch waiting entrants
        QuerySnapshot waitingSnapshot = getEntrantsByStatus("waiting");
        assertFalse("There should be a waiting entrant to promote", waitingSnapshot.isEmpty());

        // Step 3: promote the first one (only one exists)
        Tasks.await(
                waitingSnapshot.getDocuments().get(0).getReference()
                        .update("status", "selected"),
                10, TimeUnit.SECONDS
        );

        // Verify
        assertEquals("cancelled", getEntrantDoc("e001").getString("status"));
        assertEquals("selected",  getEntrantDoc("e002").getString("status"));
    }

    @Test
    public void afterCancel_multipleWaiting_exactlyOneGetsPromoted() throws Exception {
        seedEntrantWithStatus("e001", "selected");
        seedEntrantWithStatus("e002", "waiting");
        seedEntrantWithStatus("e003", "waiting");
        seedEntrantWithStatus("e004", "waiting");

        // Cancel e001
        Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document("e001")
                        .update("status", "cancelled"),
                10, TimeUnit.SECONDS
        );

        // Promote one random waiting entrant
        QuerySnapshot waitingSnapshot = getEntrantsByStatus("waiting");
        assertEquals(3, waitingSnapshot.size());

        List<DocumentSnapshot> waitingDocs = waitingSnapshot.getDocuments();
        DocumentSnapshot promoted = waitingDocs.get(new java.util.Random().nextInt(waitingDocs.size()));
        Tasks.await(
                promoted.getReference().update("status", "selected"),
                10, TimeUnit.SECONDS
        );

        // Verify exactly one selected exists
        QuerySnapshot selectedSnapshot = getEntrantsByStatus("selected");
        assertEquals(1, selectedSnapshot.size());

        // Verify exactly two waiting remain
        QuerySnapshot remainingWaiting = getEntrantsByStatus("waiting");
        assertEquals(2, remainingWaiting.size());
    }

    @Test
    public void afterCancel_noWaitingEntrants_noneGetPromoted() throws Exception {
        seedEntrantWithStatus("e001", "selected");
        seedEntrantWithStatus("e002", "accepted");
        seedEntrantWithStatus("e003", "rejected");

        // Cancel e001
        Tasks.await(
                db.collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(EVENT_DOC_ID)
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .document("e001")
                        .update("status", "cancelled"),
                10, TimeUnit.SECONDS
        );

        // No waiting entrants — nothing should be promoted
        QuerySnapshot waitingSnapshot = getEntrantsByStatus("waiting");
        assertTrue("No waiting entrants should exist", waitingSnapshot.isEmpty());

        // Accepted and rejected should be unchanged
        assertEquals("accepted", getEntrantDoc("e002").getString("status"));
        assertEquals("rejected", getEntrantDoc("e003").getString("status"));

        // No selected entrants should exist
        QuerySnapshot selectedSnapshot = getEntrantsByStatus("selected");
        assertTrue("No one should have been promoted to selected", selectedSnapshot.isEmpty());
    }

    // ── updateEntrantStatus via DatabaseHandler ───────────────────────────────

    @Test
    public void updateEntrantStatus_writesCorrectStatusToFirestore() throws Exception {
        seedEntrantWithStatus("e001", "waiting");

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", "selected");

        Tasks.await(
                dbHandler.updateEntrantStatus(EVENT_DOC_ID, "e001", statusData),
                10, TimeUnit.SECONDS
        );

        DocumentSnapshot doc = getEntrantDoc("e001");
        assertEquals("selected", doc.getString("status"));
    }

    @Test
    public void updateEntrantStatus_preservesOtherFields() throws Exception {
        seedEntrantWithStatus("e001", "waiting");

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", "selected");

        Tasks.await(
                dbHandler.updateEntrantStatus(EVENT_DOC_ID, "e001", statusData),
                10, TimeUnit.SECONDS
        );

        // entrantId and registrationTime fields should still be present
        DocumentSnapshot doc = getEntrantDoc("e001");
        assertEquals("e001", doc.getString("entrantId"));
        assertNotNull(doc.getDate("registrationTime"));
    }
}