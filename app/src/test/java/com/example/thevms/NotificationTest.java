package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.thevms.model.Notification;
import com.example.thevms.model.UserRole;

import org.junit.Test;

import java.util.Date;

public class NotificationTest {

    @Test
    public void testNotificationProperties() {
        Date now = new Date();
        Notification n = new Notification(
                "id123", "Title", "sender123", "Sender Name",
                UserRole.ORGANIZER, "receiver123", "Receiver Name",
                now, "Description", Notification.TYPE_GENERAL, "event123"
        );

        assertEquals("id123", n.getId());
        assertEquals("Title", n.getTitle());
        assertEquals("sender123", n.getSenderId());
        assertEquals("Sender Name", n.getSenderName());
        assertEquals(UserRole.ORGANIZER, n.getSenderRole());
        assertEquals("receiver123", n.getReceiverId());
        assertEquals("Receiver Name", n.getReceiverName());
        assertEquals(now, n.getTimestamp());
        assertEquals("Description", n.getDescription());
        assertEquals(Notification.TYPE_GENERAL, n.getType());
        assertEquals("event123", n.getEventId());
    }

    @Test
    public void testCreateLotteryWin() {
        Notification win = Notification.createLotteryWin("sId", "Org", "rId", "User", "eId", "My Event");
        assertEquals("Lottery Results", win.getTitle());
        assertEquals(Notification.TYPE_INVITE, win.getType());
        assertTrue(win.getDescription().contains("My Event"));
        assertTrue(win.getDescription().contains("selected"));
        assertEquals("eId", win.getEventId());
    }

    @Test
    public void testCreateLotteryLoss() {
        Notification loss = Notification.createLotteryLoss("sId", "Org", "rId", "User", "eId", "My Event");
        assertEquals("Lottery Results", loss.getTitle());
        assertEquals(Notification.TYPE_GENERAL, loss.getType());
        assertTrue(loss.getDescription().contains("My Event"));
        assertTrue(loss.getDescription().contains("not selected"));
    }

    @Test
    public void testCreateWaitingListInvite() {
        Notification invite = Notification.createWaitingListInvite("sId", "Org", "rId", "User", "eId", "My Event");
        assertEquals("Wait List Invite", invite.getTitle());
        assertEquals(Notification.TYPE_INVITE, invite.getType());
        assertTrue(invite.getDescription().contains("waiting list"));
        assertTrue(invite.getDescription().contains("My Event"));
    }

    @Test
    public void testCreateCoOrganizerInvite() {
        Notification invite = Notification.createCoOrganizerInvite("sId", "Org Name", "rId", "User", "eId", "My Event");
        assertEquals("Co-Organizer Invite", invite.getTitle());
        assertEquals(Notification.TYPE_INVITE, invite.getType());
        assertTrue(invite.getDescription().contains("Org Name"));
        assertTrue(invite.getDescription().contains("co-organizer"));
    }
}
