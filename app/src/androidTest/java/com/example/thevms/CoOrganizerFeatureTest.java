package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Notification;
import com.example.thevms.model.UserRole;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Regression tests covering co-organizer features.
 * Moved to androidTest to provide a real Android Looper for Task continuations.
 */
@RunWith(AndroidJUnit4.class)
public class CoOrganizerFeatureTest {

    @Test
    public void coOrganizerInviteNotification_hasExpectedFields() {
        Notification invite = Notification.createCoOrganizerInvite(
                "org123",
                "Alice Organizer",
                "user456",
                "Bob Entrant",
                "E100",
                "Tech Summit");

        assertEquals("Co-Organizer Invite", invite.getTitle());
        assertEquals(Notification.TYPE_INVITE, invite.getType());
        assertEquals("org123", invite.getSenderId());
        assertEquals("user456", invite.getReceiverId());
        assertNotNull(invite.getDescription());
        assertTrue(invite.getDescription().contains("Alice Organizer"));
        assertTrue(invite.getDescription().contains("Tech Summit"));
    }

    @Test
    public void coOrganizerStatus_blocksJoinRequest() throws Exception {
        FakeDatabaseHandler fakeDb = new FakeDatabaseHandler();
        fakeDb.isCoOrganizer = true;
        fakeDb.entrantCount = 0L;

        Event event = buildEventWithHandler(fakeDb);
        event.setMaxWaitlist(5);

        Entrant entrant = new Entrant("device-1", "test@example.com", "First", "Last", null, true, UserRole.ENTRANT);

        Task<Void> task = event.addEntrant(entrant);
        try {
            Tasks.await(task);
            fail("Expected co-organizer join attempt to fail");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof IllegalStateException);
            assertTrue(ex.getCause().getMessage().contains("Co-organizers"));
        }
    }

    @Test
    public void normalEntrant_getsWaitingStatusRecorded() throws Exception {
        FakeDatabaseHandler fakeDb = new FakeDatabaseHandler();
        fakeDb.isCoOrganizer = false;
        fakeDb.entrantCount = 1L;

        Event event = buildEventWithHandler(fakeDb);
        event.setMaxWaitlist(10);

        Entrant entrant = new Entrant("device-2", "user@example.com", "John", "Doe", null, true, UserRole.ENTRANT);

        Task<Void> task = event.addEntrant(entrant);
        Tasks.await(task);

        assertEquals("42", fakeDb.lastUpdatedEventId);
        assertEquals("device-2", fakeDb.lastUpdatedUserId);
        assertEquals(DatabaseHandler.STATUS_WAITING, fakeDb.lastStatusData.get("status"));
    }

    private Event buildEventWithHandler(FakeDatabaseHandler handler) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", 42L);
        data.put("name", "Test Event");
        Event event = Event.fromMap(data);

        Field field = Event.class.getDeclaredField("dbHandler");
        field.setAccessible(true);
        field.set(event, handler);
        return event;
    }

    private static class FakeDatabaseHandler extends DatabaseHandler {
        boolean isCoOrganizer;
        long entrantCount;
        Map<String, Object> lastStatusData;
        String lastUpdatedEventId;
        String lastUpdatedUserId;

        @Override
        public com.google.android.gms.tasks.Task<Boolean> isCoOrganizer(String eventId, String userId) {
            return Tasks.forResult(isCoOrganizer);
        }

        @Override
        public com.google.android.gms.tasks.Task<Long> getEntrantCount(String eventId) {
            return Tasks.forResult(entrantCount);
        }

        @Override
        public com.google.android.gms.tasks.Task<Void> updateEntrantStatus(String eventId, String userId, Map<String, Object> statusData) {
            this.lastUpdatedEventId = eventId;
            this.lastUpdatedUserId = userId;
            this.lastStatusData = statusData;
            return Tasks.forResult(null);
        }
    }
}
