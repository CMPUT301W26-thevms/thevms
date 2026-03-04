package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.ui.SettingsActivity;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * UI tests for SettingsActivity.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {

    private FirestoreTestHelper testHelper;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        deviceId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Seed a test user
        Entrant user = new Entrant(deviceId, "old@example.com", "OldFirstName", "OldLastName", "1112223333");
        Tasks.await(user.save(), 5, TimeUnit.SECONDS);
    }

    @Test
    public void testChangeFirstName() throws Exception {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_first_name)).perform(replaceText("NewFirstName"), closeSoftKeyboard());
            onView(withId(R.id.btn_confirm)).perform(click());
            onView(withText("Yes")).perform(click());

            // Verify database update
            Entrant updated = Tasks.await(Entrant.getOrCreate(deviceId), 5, TimeUnit.SECONDS);
            assertEquals("NewFirstName", updated.getFirstName());
        }
    }

    @Test
    public void testChangeLastName() throws Exception {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_last_name)).perform(replaceText("NewLastName"), closeSoftKeyboard());
            onView(withId(R.id.btn_confirm)).perform(click());
            onView(withText("Yes")).perform(click());

            // Verify database update
            Entrant updated = Tasks.await(Entrant.getOrCreate(deviceId), 5, TimeUnit.SECONDS);
            assertEquals("NewLastName", updated.getLastName());
        }
    }

    @Test
    public void testChangeEmail_Valid() throws Exception {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_email)).perform(replaceText("new@example.com"), closeSoftKeyboard());
            onView(withId(R.id.btn_confirm)).perform(click());
            onView(withText("Yes")).perform(click());

            // Verify database update
            Entrant updated = Tasks.await(Entrant.getOrCreate(deviceId), 5, TimeUnit.SECONDS);
            assertEquals("new@example.com", updated.getEmail());
        }
    }

    @Test
    public void testChangeEmail_Invalid() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_email)).perform(replaceText("not-an-email"), closeSoftKeyboard());
            onView(withId(R.id.btn_confirm)).perform(click());

            onView(withId(R.id.edit_email)).check(matches(hasErrorText("Invalid email address")));
        }
    }

    @Test
    public void testChangePhoneNumber_Valid() throws Exception {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_phone)).perform(replaceText("9998887777"), closeSoftKeyboard());
            onView(withId(R.id.btn_confirm)).perform(click());
            onView(withText("Yes")).perform(click());

            // Verify database update
            Entrant updated = Tasks.await(Entrant.getOrCreate(deviceId), 5, TimeUnit.SECONDS);
            assertEquals("9998887777", updated.getPhoneNumber());
        }
    }

    @Test
    public void testChangePhoneNumber_Invalid() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_phone)).perform(replaceText("abcd"), closeSoftKeyboard());
            onView(withId(R.id.btn_confirm)).perform(click());

            onView(withId(R.id.edit_phone)).check(matches(hasErrorText("Invalid phone number")));
        }
    }

    @Test
    public void testChangeMultipleFields() throws Exception {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.edit_first_name)).perform(replaceText("John"), closeSoftKeyboard());
            onView(withId(R.id.edit_last_name)).perform(replaceText("Doe"), closeSoftKeyboard());
            onView(withId(R.id.edit_email)).perform(replaceText("john.doe@example.com"), closeSoftKeyboard());
            onView(withId(R.id.edit_phone)).perform(replaceText("5556667777"), closeSoftKeyboard());

            onView(withId(R.id.btn_confirm)).perform(click());
            onView(withText("Yes")).perform(click());

            // Verify all fields in database
            Entrant updated = Tasks.await(Entrant.getOrCreate(deviceId), 5, TimeUnit.SECONDS);
            assertEquals("John", updated.getFirstName());
            assertEquals("Doe", updated.getLastName());
            assertEquals("john.doe@example.com", updated.getEmail());
            assertEquals("5556667777", updated.getPhoneNumber());
        }
    }
}
