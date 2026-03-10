package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OrganizerTest {

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

    @Test
    public void testOrganizerListInitialization() {
        Organizer organizer = new Organizer("id", "email", "first", "last", "phone");
        assertNotNull(organizer.getRole());
    }
}
