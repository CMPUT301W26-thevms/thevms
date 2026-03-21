package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.google.android.gms.tasks.Task;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Date;

public class EventTest {

    @Test
    public void testAddEntrantTooEarly() throws Exception {
        // Since Event constructor is private, we use reflection for the unit test
        // to avoid mocking the database for a simple logic check.
        Constructor<Event> constructor = Event.class.getDeclaredConstructor(
                Long.class, String.class, String.class, 
                com.example.thevms.model.Organizer.class, 
                String.class, String.class,
                Date.class, Date.class, Date.class, Date.class,
                boolean.class, Double.class, android.location.Location.class
        );
        constructor.setAccessible(true);

        // Set registration start time to 1 hour in the future
        Date futureStart = new Date(System.currentTimeMillis() + 3600000);
        Date futureEnd = new Date(System.currentTimeMillis() + 7200000);

        Event event = constructor.newInstance(
                1L, "Test Event", "Description", null, null, null,
                futureStart, futureEnd, null, null, false, null, null
        );

        Entrant entrant = new Entrant("device123", "test@example.com", "First", "Last", null);

        Task<Void> task = event.addEntrant(entrant);

        assertTrue("Task should be completed", task.isComplete());
        assertTrue("Task should have failed", !task.isSuccessful());
        
        Exception e = task.getException();
        assertTrue("Exception should be IllegalStateException", e instanceof IllegalStateException);
        assertEquals("Registration has not started yet.", e.getMessage());
    }

    @Test
    public void testAddEntrantTooLate() throws Exception {
        Constructor<Event> constructor = Event.class.getDeclaredConstructor(
                Long.class, String.class, String.class,
                com.example.thevms.model.Organizer.class,
                String.class, String.class,
                Date.class, Date.class, Date.class, Date.class,
                boolean.class, Double.class, android.location.Location.class
        );
        constructor.setAccessible(true);

        // Set registration end time to 1 hour in the past
        Date pastStart = new Date(System.currentTimeMillis() - 7200000);
        Date pastEnd = new Date(System.currentTimeMillis() - 3600000);

        Event event = constructor.newInstance(
                1L, "Test Event", "Description", null, null, null,
                pastStart, pastEnd, null, null, false, null, null
        );


        Entrant entrant = new Entrant("device123", "test@example.com", "First", "Last", null);

        Task<Void> task = event.addEntrant(entrant);

        assertTrue("Task should be completed", task.isComplete());
        assertTrue("Task should have failed", !task.isSuccessful());
        
        Exception e = task.getException();
        assertTrue("Exception should be IllegalStateException", e instanceof IllegalStateException);
        assertEquals("Registration has ended.", e.getMessage());
    }
}
