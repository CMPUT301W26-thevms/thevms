package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.ui.MyEventsActivity;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
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
        seedUserAndEntrant(event.getEventId(), "user1", "John", "Doe", 53.5232, -113.5263);
        seedUserAndEntrant(event.getEventId(), "user2", "Jane", "Smith", 53.5245, -113.5208);
        seedUserAndEntrant(event.getEventId(), "user3", "Bob", "Johnson", 53.5271, -113.5174);
    }

    private void seedUserAndEntrant(long eventId, String userId, String firstName, String lastName,
                                    double lat, double lng) throws Exception {
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
        registrationData.put("entrantLat", lat);
        registrationData.put("entrantLng", lng);

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

    @Test
    public void testShowQrCodeDialog() throws InterruptedException {
        // Wait for data to load
        Thread.sleep(2000);

        // Check if the QR button is visible
        onView(withId(R.id.rv_my_events))
                .check(matches(MainActivityTest.hasDescendant(withId(R.id.btn_show_qr))));

        // Click the "Show QR" button
        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_show_qr)));

        // Wait for dialog to appear
        Thread.sleep(1000);

        // Verify that the dialog is displayed with the correct title and the QR image
        onView(withText("My Test Event QR Code")).check(matches(isDisplayed()));
        onView(withId(R.id.iv_qr_code)).check(matches(isDisplayed()));

        // Close the dialog
        onView(withId(R.id.btn_close_qr)).perform(click());
    }

    @Test
    public void testPrivateEventShowQrNotVisible() throws Exception {
        // Close scenario from setUp
        scenario.close();
        testHelper.clearDatabase();

        // Seed a private event
        Organizer organizer = new Organizer(deviceId, "org@example.com", "Main", "Organizer", null);
        Event event = Tasks.await(Event.create(
                "Private Event",
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
                true // isPrivate = true
        ), 10, TimeUnit.SECONDS);
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);

        // Relaunch scenario
        scenario = ActivityScenario.launch(MyEventsActivity.class);
        Thread.sleep(2000);

        // Verify that btn_show_qr is NOT visible for private event
        onView(withId(R.id.rv_my_events))
                .check(matches(not(MainActivityTest.hasDescendant(allOf(withId(R.id.btn_show_qr), isDisplayed())))));
    }

    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View target = view.findViewById(id);
                if (target != null) {
                    target.performClick();
                }
            }
        };
    }

}
