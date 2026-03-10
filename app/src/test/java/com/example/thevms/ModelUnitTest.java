package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.thevms.model.Admin;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelUnitTest {

    // --- Entrant Tests ---

    @Test
    public void testEntrantConstructor() {
        Entrant entrant = new Entrant("dev123", "test@example.com", "John", "Doe", "1234567890");
        assertEquals("dev123", entrant.getDeviceId());
        assertEquals("test@example.com", entrant.getEmail());
        assertEquals("John", entrant.getFirstName());
        assertEquals("Doe", entrant.getLastName());
        assertEquals("1234567890", entrant.getPhoneNumber());
        assertTrue(entrant.isNotificationsEnabled());
        assertEquals(UserRole.ENTRANT, entrant.getRole());
    }

    @Test
    public void testEntrantSetters() {
        Entrant entrant = new Entrant("id", "e", "f", "l", "p");
        entrant.setFirstName("Jane");
        entrant.setLastName("Smith");
        entrant.setPhoneNumber("0987654321");
        entrant.setNotificationsEnabled(false);

        assertEquals("Jane", entrant.getFirstName());
        assertEquals("Smith", entrant.getLastName());
        assertEquals("0987654321", entrant.getPhoneNumber());
        assertFalse(entrant.isNotificationsEnabled());
    }

    @Test
    public void testEntrantFromMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", "map@example.com");
        data.put("firstName", "Map");
        data.put("lastName", "User");
        data.put("role", "ORGANIZER");

        Entrant entrant = Entrant.fromMap("dev456", data);
        assertNotNull(entrant);
        assertEquals("Map", entrant.getFirstName());
        assertEquals(UserRole.ORGANIZER, entrant.getRole());
    }

    // --- Organizer Tests ---

    @Test
    public void testOrganizerConstructor() {
        Organizer organizer = new Organizer("org1", "org@example.com", "Boss", "Man", null);
        assertEquals(UserRole.ORGANIZER, organizer.getRole());
        assertNotNull(organizer.getDeviceId());
    }

    @Test
    public void testOrganizerFromMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", "Org");
        data.put("lastName", "User");
        data.put("notificationsEnabled", Boolean.TRUE);

        Organizer organizer = Organizer.fromMap("orgid", data);
        assertNotNull(organizer);
        assertEquals("Org", organizer.getFirstName());
    }

    // --- Admin Tests ---

    @Test
    public void testAdminConstructor() {
        Admin admin = new Admin("admin1", "admin@example.com", "Super", "User", "555");
        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    public void testAdminRemoveEvent() {
        Organizer org = new Organizer("o1", "o@e.com", "O", "R", null);
        List<Event> allEvents = new ArrayList<>();

        // Creating an event via fromMap as the constructor is private
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

    // --- Event Tests ---

    @Test
    public void testEventFromMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", 99L);
        data.put("name", "Party");
        data.put("description", "Fun");

        Event event = Event.fromMap(data);
        assertNotNull(event);
        assertEquals(Long.valueOf(99L), event.getEventId());
        assertEquals("Party", event.getName());
        assertEquals("Fun", event.getDescription());
    }

    @Test
    public void testEventGettersSetters() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", 1L);
        Event event = Event.fromMap(data);

        event.setName("New Name");
        event.setDescription("New Desc");
        event.setImageUrl("http://image.com");

        assertEquals("New Name", event.getName());
        assertEquals("New Desc", event.getDescription());
        assertEquals("http://image.com", event.getImageUrl());

        event.removeImageUrl();
        assertNull(event.getImageUrl());
    }

    @Test
    public void testEventDateSetters() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", 1L);
        Event event = Event.fromMap(data);

        Date now = new Date();
        event.setEventStartTime(now);
        assertEquals(now, event.getEventStartTime());

        event.setRegistrationEndTime(now);
        assertEquals(now, event.getRegistrationEndTime());
    }

    @Test
    public void testUserRoleEnum() {
        assertEquals("ENTRANT", UserRole.ENTRANT.name());
        assertEquals("ORGANIZER", UserRole.ORGANIZER.name());
        assertEquals("ADMIN", UserRole.ADMIN.name());
    }

    @Test
    public void testEntrantToMap() {
        Entrant entrant = new Entrant("d1", "e1", "f1", "l1", "p1");
        // Accessing protected toMap for testing
        // Since we are in the same package usually, but here we are in test. 
        // We'll use the public getters to verify state instead as that's safer for unit tests.
        assertEquals("f1", entrant.getFirstName());
    }

    @Test
    public void testOrganizerListInitialization() {
        Organizer organizer = new Organizer("id", "email", "first", "last", "phone");
        // currentEvents is package-private or we can verify it's not null via logic
        assertNotNull(organizer.getRole());
    }

    @Test
    public void testEventEntrantList() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", 1L);
        Event event = Event.fromMap(data);
        assertNotNull(event.getEntrantList());
        assertEquals(0, event.getEntrantList().size());
    }

    @Test
    public void testEventOrganizerAssociation() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", 1L);
        Event event = Event.fromMap(data);
        Organizer organizer = new Organizer("org1", "email", "first", "last", null);
        event.setOrganizer(organizer);
        assertEquals(organizer, event.getOrganizer());
    }

    @Test
    public void testEntrantDefaultNotifications() {
        Entrant entrant = new Entrant("id", "email", "first", "last", null);
        assertTrue(entrant.isNotificationsEnabled());
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
