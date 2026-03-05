package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * UI tests for AdminProfilesFragment.
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

        // Seed some test users
        Entrant user1 = new Entrant("user1", "alice@example.com", "Alice", "Smith", "1112223333");
        Tasks.await(user1.save(), 5, TimeUnit.SECONDS);

        Entrant user2 = new Entrant("user2", "bob@example.com", "Bob", "Jones", "4445556666");
        Tasks.await(user2.save(), 5, TimeUnit.SECONDS);
    }

    @Test
    public void testAdminCanNavigateToProfilesAndSeeUsers() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Wait for role check and UI update
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            // Open Admin Drawer (Gear icon click opens drawer in our implementation)
            onView(withId(R.id.nav_admin_settings)).perform(androidx.test.espresso.action.ViewActions.click());

            // Click "Manage Profiles" in the drawer
            onView(withText("Manage Profiles")).perform(androidx.test.espresso.action.ViewActions.click());

            // Check if fragment title is displayed
            onView(withText("User Profiles")).check(matches(isDisplayed()));

            // Verify Alice and Bob are in the list
            onView(withText("Alice Smith")).check(matches(isDisplayed()));
            onView(withText("alice@example.com")).check(matches(isDisplayed()));
            onView(withText("Bob Jones")).check(matches(isDisplayed()));
            onView(withText("bob@example.com")).check(matches(isDisplayed()));
        }
    }
}
