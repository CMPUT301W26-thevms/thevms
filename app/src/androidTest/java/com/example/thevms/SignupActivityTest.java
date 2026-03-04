package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.annotation.SuppressLint;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SignupActivity;
import com.google.android.gms.tasks.Tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * UI tests for SignupActivity.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class SignupActivityTest {

    private FirestoreTestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        Intents.init();
        testHelper = new FirestoreTestHelper();
        // Database is cleared HERE, before any activity is launched in the tests below.
        testHelper.clearDatabase();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testInvalidSignup_MissingFields() {
        try (ActivityScenario<SignupActivity> scenario = ActivityScenario.launch(SignupActivity.class)) {
            // Leave fields empty and click signup
            onView(withId(R.id.btn_signup)).perform(click());

            // Check for error on first name (validation stops at the first error)
            onView(withId(R.id.edit_first_name)).check(matches(hasErrorText("First name is required")));
        }
    }

    @Test
    public void testInvalidSignup_InvalidEmail() {
        try (ActivityScenario<SignupActivity> scenario = ActivityScenario.launch(SignupActivity.class)) {
            onView(withId(R.id.edit_first_name)).perform(typeText("John"), closeSoftKeyboard());
            onView(withId(R.id.edit_last_name)).perform(typeText("Doe"), closeSoftKeyboard());
            onView(withId(R.id.edit_email)).perform(typeText("not-an-email"), closeSoftKeyboard());

            onView(withId(R.id.btn_signup)).perform(click());

            onView(withId(R.id.edit_email)).check(matches(hasErrorText("Invalid email address")));
        }
    }

    @Test
    public void testInvalidSignup_InvalidPhone() {
        try (ActivityScenario<SignupActivity> scenario = ActivityScenario.launch(SignupActivity.class)) {
            onView(withId(R.id.edit_first_name)).perform(typeText("John"), closeSoftKeyboard());
            onView(withId(R.id.edit_last_name)).perform(typeText("Doe"), closeSoftKeyboard());
            onView(withId(R.id.edit_email)).perform(typeText("john.doe@example.com"), closeSoftKeyboard());
            onView(withId(R.id.edit_phone)).perform(typeText("not-a-phone"), closeSoftKeyboard());

            onView(withId(R.id.btn_signup)).perform(click());

            onView(withId(R.id.edit_phone)).check(matches(hasErrorText("Invalid phone number")));
        }
    }

    @Test
    public void testValidSignup() {
        try (ActivityScenario<SignupActivity> scenario = ActivityScenario.launch(SignupActivity.class)) {
            onView(withId(R.id.edit_first_name)).perform(typeText("Jane"), closeSoftKeyboard());
            onView(withId(R.id.edit_last_name)).perform(typeText("Smith"), closeSoftKeyboard());
            onView(withId(R.id.edit_email)).perform(typeText("jane.smith@example.com"), closeSoftKeyboard());
            onView(withId(R.id.edit_phone)).perform(typeText("1234567890"), closeSoftKeyboard());

            onView(withId(R.id.btn_signup)).perform(click());

            // Verify navigation to MainActivity
            intended(hasComponent(MainActivity.class.getName()));
        }
    }

    @Test
    public void testAutoLogin() throws Exception {
        // 1. Manually create a user in the test DB for the current device BEFORE launching the activity
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Entrant existingUser = new Entrant(deviceId, "auto@login.com", "Auto", "Login", null);
        Tasks.await(existingUser.save(), 5, TimeUnit.SECONDS);

        // 2. Launch the activity - it will now find the user we just seeded and auto-login
        try (ActivityScenario<SignupActivity> scenario = ActivityScenario.launch(SignupActivity.class)) {
            // Verify it immediately navigates away from Signup to MainActivity
            intended(hasComponent(MainActivity.class.getName()));
        }
    }
}
