package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * UI tests for Admin Profile management.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class AdminProfilesTest {

    private FirestoreTestHelper testHelper;
    private String adminDeviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = ApplicationProvider.getApplicationContext();
        adminDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed an admin user so the gear icon is visible
        Entrant admin = new Entrant(adminDeviceId, "admin@example.com", "Admin", "User", "1234567890", true, UserRole.ADMIN);
        Tasks.await(admin.save(), 5, TimeUnit.SECONDS);

        // Seed a test user (Entrant)
        Entrant user1 = new Entrant("user1", "alice@example.com", "Alice", "Smith", "1112223333");
        Tasks.await(user1.save(), 5, TimeUnit.SECONDS);

        // Seed an Organizer
        Entrant organizer = new Entrant("org1", "bob@example.com", "Bob", "Jones", "4445556666", true, UserRole.ORGANIZER);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);
    }

    /**
     * Helper to wait for a view to appear, reducing flakiness from async operations like role checks.
     */
    private void waitForView(Matcher<View> matcher, int timeoutMs) throws InterruptedException {
        long timeoutMillis = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timeoutMillis) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (Exception | AssertionError e) {
                Thread.sleep(200);
            }
        }
        onView(matcher).check(matches(isDisplayed()));
    }

    /**
     * Helper to wait for a view to disappear (e.g., loading spinner).
     */
    private void waitForViewToDisappear(Matcher<View> matcher, int timeoutMs) throws InterruptedException {
        long timeoutMillis = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timeoutMillis) {
            try {
                onView(matcher).check(matches(not(isDisplayed())));
                return;
            } catch (Exception | AssertionError e) {
                Thread.sleep(200);
            }
        }
    }

    @Test
    public void testAdminCanNavigateToProfiles() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Profiles"), 2000);
            onView(withText("Manage Profiles")).perform(click());

            // Wait for the loading spinner to disappear before checking results
            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.admin_profiles_title_text)).check(matches(withText("Manage Profiles")));
            onView(withText("Alice Smith")).check(matches(isDisplayed()));
            onView(withText("Bob Jones")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAdminCanNavigateToOrganizers() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Organizers"), 2000);
            onView(withText("Manage Organizers")).perform(click());

            // IMPORTANT: Wait for the loading spinner to disappear
            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.admin_profiles_title_text)).check(matches(withText("Manage Organizers")));

            // Bob (Organizer) should be present
            onView(withText("Bob Jones")).check(matches(isDisplayed()));

            // Alice (Entrant) should be filtered out and NOT exist in the hierarchy
            onView(withText("Alice Smith")).check(doesNotExist());
        }
    }
}
