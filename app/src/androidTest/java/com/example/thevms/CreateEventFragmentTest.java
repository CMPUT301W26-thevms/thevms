package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.CreateEventFragment;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for Event Creation in CreateEventFragment.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class CreateEventFragmentTest {

    private FirestoreTestHelper testHelper;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        Intents.init();
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        deviceId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Seed an ORGANIZER user so the "Add" button is visible in MainActivity
        Entrant organizer = new Entrant(deviceId, "org@example.com", "Event", "Organizer", "123", true, UserRole.ORGANIZER);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testValidation_MissingRequiredFields() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Create Event
            onView(withId(R.id.nav_add)).perform(click());

            // Click Confirm without entering data
            onView(withId(R.id.btn_confirm)).perform(scrollTo(), click());

            // Verify error on Name field
            onView(withId(R.id.et_event_name)).check(matches(hasErrorText("Name is required")));
        }
    }

    @Test
    public void testValidation_MissingLocation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_add)).perform(click());

            onView(withId(R.id.et_event_name)).perform(replaceText("Test Event"), closeSoftKeyboard());

            onView(withId(R.id.btn_confirm)).perform(scrollTo(), click());

            onView(withId(R.id.et_event_location)).check(matches(hasErrorText("Location is required")));
        }
    }

    @Test
    public void testCreateEvent_Success() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_add)).perform(click());

            // Manually set dates using the testing helper to bypass picker UI flakiness
            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof CreateEventFragment) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.HOUR, 1);
                    Date rs = cal.getTime();
                    cal.add(Calendar.HOUR, 1);
                    Date re = cal.getTime();
                    cal.add(Calendar.HOUR, 1);
                    Date es = cal.getTime();
                    cal.add(Calendar.HOUR, 1);
                    Date ee = cal.getTime();

                    ((CreateEventFragment) fragment).setTestingDates(rs, re, es, ee);
                }
            });

            // Fill text fields
            onView(withId(R.id.et_event_name)).perform(replaceText("Annual Gala"), closeSoftKeyboard());
            onView(withId(R.id.et_event_location)).perform(replaceText("Grand Hall"), closeSoftKeyboard());
            onView(withId(R.id.et_event_description)).perform(replaceText("A night of elegance."), closeSoftKeyboard());

            // Click Confirm
            onView(withId(R.id.btn_confirm)).perform(scrollTo(), click());

            // Verify entry in database
            DatabaseHandler db = testHelper.getDbHandler();
            QuerySnapshot snapshot = Tasks.await(db.getAllEvents(), 5, TimeUnit.SECONDS);
            assertTrue("Event should exist in DB", snapshot.size() > 0);
        }
    }
}
