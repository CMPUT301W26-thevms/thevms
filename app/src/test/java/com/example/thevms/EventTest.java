package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EventTest {

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

        assertEquals("New Name", event.getName());
        assertEquals("New Desc", event.getDescription());
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
}
