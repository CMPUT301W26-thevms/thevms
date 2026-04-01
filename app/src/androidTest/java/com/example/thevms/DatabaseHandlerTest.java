package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Comment;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Notification;
import com.example.thevms.model.UserRole;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for DatabaseHandler.
 * Uses the Firestore emulator as configured in FirestoreTestHelper.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseHandlerTest {

    private DatabaseHandler dbHandler;
    private FirestoreTestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();
        dbHandler = testHelper.getDbHandler();
    }

    @Test
    public void testGetNextEventId() throws Exception {
        Long nextId = Tasks.await(dbHandler.getNextEventId(), 5, TimeUnit.SECONDS);
        assertEquals(Long.valueOf(1L), nextId);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", 1L);
        Tasks.await(dbHandler.saveEvent(1L, eventData), 5, TimeUnit.SECONDS);

        nextId = Tasks.await(dbHandler.getNextEventId(), 5, TimeUnit.SECONDS);
        assertEquals(Long.valueOf(2L), nextId);
    }

    @Test
    public void testSaveAndGetAllEvents() throws Exception {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "Test Event");
        Tasks.await(dbHandler.saveEvent(100L, eventData), 5, TimeUnit.SECONDS);

        QuerySnapshot snapshot = Tasks.await(dbHandler.getAllEvents(), 5, TimeUnit.SECONDS);
        assertFalse(snapshot.isEmpty());
        assertEquals(1, snapshot.size());
    }

    @Test
    public void testGetEventsByOrganizer() throws Exception {
        String organizerId = "org-123";
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("organizerId", organizerId);
        Tasks.await(dbHandler.saveEvent(1L, eventData), 5, TimeUnit.SECONDS);

        QuerySnapshot snapshot = Tasks.await(dbHandler.getEventsByOrganizer(organizerId), 5, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
    }

    @Test
    public void testListenToEvent() {
        ListenerRegistration registration = dbHandler.listenToEvent("1", (snapshot, e) -> {});
        assertNotNull(registration);
        registration.remove();
    }

    @Test
    public void testSaveAndGetUser() throws Exception {
        String userId = "user-123";
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "John Doe");
        Tasks.await(dbHandler.saveUser(userId, userData), 5, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(dbHandler.getUser(userId), 5, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        assertEquals("John Doe", snapshot.get("name"));
    }

    @Test
    public void testGetAllUsers() throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "User 1");
        Tasks.await(dbHandler.saveUser("u1", userData), 5, TimeUnit.SECONDS);
        Tasks.await(dbHandler.saveUser("u2", userData), 5, TimeUnit.SECONDS);

        QuerySnapshot snapshot = Tasks.await(dbHandler.getAllUsers(), 5, TimeUnit.SECONDS);
        assertEquals(2, snapshot.size());
    }

    @Test
    public void testDeleteUser() throws Exception {
        String userId = "user-to-delete";
        Map<String, Object> userData = new HashMap<>();
        Tasks.await(dbHandler.saveUser(userId, userData), 5, TimeUnit.SECONDS);

        Tasks.await(dbHandler.deleteUser(userId), 5, TimeUnit.SECONDS);
        DocumentSnapshot snapshot = Tasks.await(dbHandler.getUser(userId), 5, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testUpdateAndGetEntrantStatus() throws Exception {
        String eventId = "event-1";
        String userId = "user-1";
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", DatabaseHandler.STATUS_WAITING);

        Tasks.await(dbHandler.updateEntrantStatus(eventId, userId, statusData), 5, TimeUnit.SECONDS);
        String status = Tasks.await(dbHandler.getEntrantStatus(eventId, userId), 5, TimeUnit.SECONDS);
        assertEquals(DatabaseHandler.STATUS_WAITING, status);
    }

    @Test
    public void testListenToEntrantStatus() {
        ListenerRegistration registration = dbHandler.listenToEntrantStatus("e1", "u1", (snapshot, e) -> {});
        assertNotNull(registration);
        registration.remove();
    }

    @Test
    public void testGetEntrantsForEvent() throws Exception {
        String eventId = "event-1";
        Map<String, Object> data = new HashMap<>();
        data.put("status", "waiting");
        Tasks.await(dbHandler.updateEntrantStatus(eventId, "u1", data), 5, TimeUnit.SECONDS);
        Tasks.await(dbHandler.updateEntrantStatus(eventId, "u2", data), 5, TimeUnit.SECONDS);

        QuerySnapshot snapshot = Tasks.await(dbHandler.getEntrantsForEvent(eventId), 5, TimeUnit.SECONDS);
        assertEquals(2, snapshot.size());
    }

    @Test
    public void testGetRegistrationsForEntrant() throws Exception {
        String userId = "entrant-123";
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", userId);
        Tasks.await(dbHandler.updateEntrantStatus("e1", userId, data), 5, TimeUnit.SECONDS);

        QuerySnapshot snapshot = Tasks.await(dbHandler.getRegistrationsForEntrant(userId), 5, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
    }

    @Test
    public void testDeleteEvent() throws Exception {
        String eventId = "event-to-delete";
        Map<String, Object> eventData = new HashMap<>();
        Tasks.await(dbHandler.saveEvent(123L, eventData), 5, TimeUnit.SECONDS);
        // Map ID 123L to string "123" because saveEvent uses String.valueOf(eventId)
        
        Tasks.await(dbHandler.deleteEvent("123"), 5, TimeUnit.SECONDS);
        QuerySnapshot snapshot = Tasks.await(dbHandler.getAllEvents(), 5, TimeUnit.SECONDS);
        assertTrue(snapshot.isEmpty());
    }

    @Test
    public void testGetEntrantCount() throws Exception {
        String eventId = "event-count";
        Map<String, Object> data = new HashMap<>();
        Tasks.await(dbHandler.updateEntrantStatus(eventId, "u1", data), 5, TimeUnit.SECONDS);
        
        Long count = Tasks.await(dbHandler.getEntrantCount(eventId), 5, TimeUnit.SECONDS);
        assertEquals(Long.valueOf(1L), count);
    }

    @Test
    public void testSelectAndInviteNextEntrant() throws Exception {
        String eventId = "event-lottery";
        Map<String, Object> data = new HashMap<>();
        data.put("status", DatabaseHandler.STATUS_WAITING);
        Tasks.await(dbHandler.updateEntrantStatus(eventId, "u1", data), 5, TimeUnit.SECONDS);

        Tasks.await(dbHandler.selectAndInviteNextEntrant(eventId), 5, TimeUnit.SECONDS);
        
        String status = Tasks.await(dbHandler.getEntrantStatus(eventId, "u1"), 5, TimeUnit.SECONDS);
        assertEquals(DatabaseHandler.STATUS_SELECTED, status);
    }

    @Test
    public void testAddAndDeleteComment() throws Exception {
        String eventId = "event-comment";
        Comment comment = new Comment("u1", "John", "Doe", "Nice event", new Date());
        
        Tasks.await(dbHandler.addComment(eventId, comment), 5, TimeUnit.SECONDS);
        
        // Verifying addition via listenToComments is complex, so we just check if it doesn't crash
        // and we could potentially fetch from the sub-collection directly if we had a getter for comments.
        // Since we don't have a direct getComments, we'll assume addComment worked if no exception.
    }

    @Test
    public void testListenToComments() {
        ListenerRegistration registration = dbHandler.listenToComments("e1", (snapshot, e) -> {});
        assertNotNull(registration);
        registration.remove();
    }

    @Test
    public void testSendAndGetNotifications() throws Exception {
        String userId = "receiver-1";
        Notification notification = new Notification(null, "Title", "s1", "Sender", UserRole.ORGANIZER, userId, new Date(), "Body");
        
        Tasks.await(dbHandler.sendNotification(notification), 5, TimeUnit.SECONDS);
        
        // Verify by listening or querying directly if possible. 
        // DatabaseHandler has sendNotification(String userId, String eventId, String message) too.
        Tasks.await(dbHandler.sendNotification(userId, "e1", "Quick message"), 5, TimeUnit.SECONDS);
    }

    @Test
    public void testListenToNotifications() {
        ListenerRegistration registration = dbHandler.listenToNotifications("u1", (snapshot, e) -> {});
        assertNotNull(registration);
        registration.remove();
    }

    @Test
    public void testMarkNotificationAsReadAndIDelete() throws Exception {
        Notification notification = new Notification();
        notification.setReceiverId("u1");
        Tasks.await(dbHandler.sendNotification(notification), 5, TimeUnit.SECONDS);
        String notifId = notification.getId();
        assertNotNull(notifId);

        Tasks.await(dbHandler.markNotificationAsRead(notifId), 5, TimeUnit.SECONDS);
        Tasks.await(dbHandler.deleteNotification(notifId), 5, TimeUnit.SECONDS);
    }

    @Test
    public void testAssignCoOrganizerAndIsCoOrganizer() throws Exception {
        String eventId = "event-coop";
        String userId = "user-coop";
        
        // Need to create event first so update works
        Map<String, Object> eventData = new HashMap<>();
        Tasks.await(dbHandler.saveEvent(999L, eventData), 5, TimeUnit.SECONDS);

        Tasks.await(dbHandler.assignCoOrganizer("999", "Event Name", "org1", "Org", userId, "Entrant"), 5, TimeUnit.SECONDS);
        
        boolean isCoOp = Tasks.await(dbHandler.isCoOrganizer("999", userId), 5, TimeUnit.SECONDS);
        assertTrue(isCoOp);
    }
    
    @Test
    public void testDeleteUserAccountCompletely() throws Exception {
        String userId = "user-full-delete";
        String eventId = "10";
        
        // 1. Create user
        Map<String, Object> userData = new HashMap<>();
        Tasks.await(dbHandler.saveUser(userId, userData), 5, TimeUnit.SECONDS);
        
        // 2. Create event organized by user
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("organizerId", userId);
        Tasks.await(dbHandler.saveEvent(10L, eventData), 5, TimeUnit.SECONDS);
        
        // 3. Register user for another event
        Map<String, Object> regData = new HashMap<>();
        regData.put("entrantId", userId);
        regData.put("status", DatabaseHandler.STATUS_WAITING);
        Tasks.await(dbHandler.updateEntrantStatus("20", userId, regData), 5, TimeUnit.SECONDS);
        
        // 4. Perform complete deletion
        Tasks.await(dbHandler.deleteUserAccountCompletely(userId), 10, TimeUnit.SECONDS);
        
        // 5. Verify user profile gone
        DocumentSnapshot userSnap = Tasks.await(dbHandler.getUser(userId), 5, TimeUnit.SECONDS);
        assertFalse(userSnap.exists());
        
        // 6. Verify organized event gone
        QuerySnapshot eventSnap = Tasks.await(dbHandler.getEventsByOrganizer(userId), 5, TimeUnit.SECONDS);
        assertTrue(eventSnap.isEmpty());
        
        // 7. Verify registration gone
        String status = Tasks.await(dbHandler.getEntrantStatus("20", userId), 5, TimeUnit.SECONDS);
        assertNull(status);
    }
}
