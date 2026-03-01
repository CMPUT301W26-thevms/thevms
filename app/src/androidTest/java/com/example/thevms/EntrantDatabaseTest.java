package com.example.thevms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for Entrant database operations.
 * Requires the Firestore emulator to be running.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantDatabaseTest {

    private FirestoreTestHelper testHelper;
    private static final String TEST_DEVICE_ID = "test-device-123";

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();
    }

    @Test
    public void testCreateAndSaveUser() throws Exception {
        Entrant entrant = new Entrant(TEST_DEVICE_ID, "test@example.com", "John", "Doe", "1234567890");
        
        // Save to database
        Tasks.await(entrant.save(), 5, TimeUnit.SECONDS);

        // Fetch back to verify
        Entrant fetched = Tasks.await(Entrant.getOrCreate(TEST_DEVICE_ID), 5, TimeUnit.SECONDS);

        assertNotNull(fetched);
        assertEquals("test@example.com", fetched.getEmail());
        assertEquals("John", fetched.getFirstName());
        assertEquals("Doe", fetched.getLastName());
        assertEquals("1234567890", fetched.getPhoneNumber());
    }

    @Test
    public void testGetOrCreateNewUser() throws Exception {
        // Fetch a user that doesn't exist yet
        Entrant newEntrant = Tasks.await(Entrant.getOrCreate("non-existent-id"), 5, TimeUnit.SECONDS);

        assertNotNull(newEntrant);
        assertEquals("non-existent-id", newEntrant.getDeviceId());
        assertNull(newEntrant.getEmail()); // Should be empty/new
        assertNull(newEntrant.getFirstName());
    }

    @Test
    public void testGetOrCreateExistingUser() throws Exception {
        // Manually create a user first
        Entrant entrant = new Entrant(TEST_DEVICE_ID, "existing@example.com", "Jane", "Smith", null);
        Tasks.await(entrant.save(), 5, TimeUnit.SECONDS);

        // Fetch again using getOrCreate
        Entrant fetched = Tasks.await(Entrant.getOrCreate(TEST_DEVICE_ID), 5, TimeUnit.SECONDS);

        assertNotNull(fetched);
        assertEquals("existing@example.com", fetched.getEmail());
        assertEquals("Jane", fetched.getFirstName());
        assertEquals("Smith", fetched.getLastName());
        assertNull(fetched.getPhoneNumber());
    }
}
