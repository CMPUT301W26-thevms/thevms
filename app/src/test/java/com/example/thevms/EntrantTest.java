package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EntrantTest {

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

    @Test
    public void testEntrantToMap() {
        Entrant entrant = new Entrant("d1", "e1", "f1", "l1", "p1");
        assertEquals("f1", entrant.getFirstName());
    }

    @Test
    public void testEntrantDefaultNotifications() {
        Entrant entrant = new Entrant("id", "email", "first", "last", null);
        assertTrue(entrant.isNotificationsEnabled());
    }
}
