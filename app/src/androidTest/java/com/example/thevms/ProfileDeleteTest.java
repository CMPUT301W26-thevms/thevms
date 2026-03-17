package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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
 * UI test for deleting own profile from ProfileFragment.
 * Ensures the app redirects to the signup page after deletion.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileDeleteTest {

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

        // Seed a test user so we can bypass the initial signup screen and reach the ProfileFragment
        Entrant user = new Entrant(deviceId, "test@example.com", "Test", "User", "1234567890");
        Tasks.await(user.save(), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testDeleteOwnProfileRedirectsToSignup() {
        // Launch MainActivity directly since we seeded a user
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            
            // 1. Navigate to Profile tab
            onView(withId(R.id.nav_profile)).perform(click());

            // 2. Click Delete Profile button
            // Added scrollTo() because the button is inside a NestedScrollView and may be off-screen
            onView(withId(R.id.btn_delete_profile)).perform(scrollTo(), click());

            // 3. Verify confirmation dialog is displayed
            onView(withText("Delete Profile")).check(matches(isDisplayed()));
            
            // 4. Confirm deletion
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            // 5. Verify redirection to SignupActivity
            // We use intent verification to confirm the redirection
            intended(hasComponent(SignupActivity.class.getName()));
        }
    }
}
