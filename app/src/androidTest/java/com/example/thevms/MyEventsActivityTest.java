package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.ui.MyEventsActivity;
import com.google.android.gms.tasks.Tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyEventsActivityTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MyEventsActivity> scenario;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed an event for the current device (organizer)
        seedEventForCurrentDevice();

        scenario = ActivityScenario.launch(MyEventsActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    private void seedEventForCurrentDevice() throws Exception {
        Organizer organizer = new Organizer(deviceId, "org@example.com", "Main", "Organizer", null);
        Event event = Tasks.await(Event.create(
                "My Test Event",
                "Description",
                organizer,
                null,
                null,
                new Date(),
                new Date(System.currentTimeMillis() + 86400000),
                new Date(),
                new Date(System.currentTimeMillis() + 3600000),
                false,
                0.0,
                null,
                false
        ), 10, TimeUnit.SECONDS);
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);

        // Seed users who signed up
        seedUserAndEntrant(event.getEventId(), "user1", "John", "Doe");
        seedUserAndEntrant(event.getEventId(), "user2", "Jane", "Smith");
        seedUserAndEntrant(event.getEventId(), "user3", "Bob", "Johnson");
    }

    private void seedUserAndEntrant(long eventId, String userId, String firstName, String lastName) throws Exception {
        DatabaseHandler db = testHelper.getDbHandler();
        
        // 1. Create User Profile
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", userId + "@example.com");
        userData.put("role", "ENTRANT");
        Tasks.await(db.saveUser(userId, userData), 10, TimeUnit.SECONDS);

        // 2. Register User in Event
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("entrantId", userId);
        registrationData.put("status", "waiting");
        registrationData.put("registrationTime", new Date());
        
        Tasks.await(db.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(userId)
                .set(registrationData), 10, TimeUnit.SECONDS);
    }

    @Test
    public void testAttendeesNamesVisible() throws InterruptedException {
        // Wait for data to load
        Thread.sleep(2000);

        // Check event name
        onView(withText("My Test Event")).check(matches(isDisplayed()));

        // Check if all attendees' full names are displayed in the list
        onView(withText("John Doe")).check(matches(isDisplayed()));
        onView(withText("Jane Smith")).check(matches(isDisplayed()));
        onView(withText("Bob Johnson")).check(matches(isDisplayed()));
    }
}
