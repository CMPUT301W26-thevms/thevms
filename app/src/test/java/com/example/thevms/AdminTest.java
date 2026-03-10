package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.thevms.model.Admin;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminTest {

    @Test
    public void testAdminConstructor() {
        Admin admin = new Admin("admin1", "admin@example.com", "Super", "User", "555");
        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    public void testAdminRemoveEvent() {
        Organizer org = new Organizer("o1", "o@e.com", "O", "R", null);
        List<Event> allEvents = new ArrayList<>();

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", 1L);
        eventData.put("name", "Test Event");
        Event event = Event.fromMap(eventData);
        event.setOrganizer(org);

        allEvents.add(event);
        assertEquals(1, allEvents.size());

        Admin admin = new Admin("a1", "a@e.com", "A", "D", null);
        admin.removeEvent(event, allEvents);

        assertTrue(allEvents.isEmpty());
    }

    @Test
    public void testAdminFromMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", "Admin");
        data.put("notificationsEnabled", Boolean.TRUE);

        Admin admin = Admin.fromMap("id", data);
        assertNotNull(admin);
        assertEquals(UserRole.ADMIN, admin.getRole());
    }
}
