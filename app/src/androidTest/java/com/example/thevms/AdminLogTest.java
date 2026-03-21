package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Notification;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for the Admin Logs functionality.
 */
@RunWith(AndroidJUnit4.class)
public class AdminLogTest {

    private FirestoreTestHelper testHelper;
    private String adminDeviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        // Give the emulator/listeners a moment to settle after clearing
        Thread.sleep(500);

        Context context = ApplicationProvider.getApplicationContext();
        adminDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed an admin user so the gear icon is visible in navigation
        Entrant admin = Tasks.await(Entrant.getOrCreate(adminDeviceId), 5, TimeUnit.SECONDS);
        admin.setRole(UserRole.ADMIN);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        Tasks.await(admin.save(), 5, TimeUnit.SECONDS);
    }

    private void seedNotification(Notification notification) throws Exception {
        Tasks.await(notification.send(), 5, TimeUnit.SECONDS);
    }

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

    @Test
    public void testLogFieldsDisplayCorrectly() throws Exception {
        String receiverName = "Recipient Entrant";
        String senderName = "Test Organizer";
        String title = "Log Test Title";
        String description = "This is a log message for testing.";

        // Seed a notification
        Notification logEntry = new Notification(
                null,
                title,
                "sender_id",
                senderName,
                UserRole.ORGANIZER,
                "receiver_id",
                receiverName,
                new Date(),
                description,
                Notification.TYPE_INVITE, // Invite type should still NOT show buttons in read-only mode
                null
        );
        seedNotification(logEntry);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Admin Panel (Gear icon)
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            // Click View Logs in the drawer
            waitForView(withText("View Logs"), 2000);
            onView(withText("View Logs")).perform(click());

            // Verify all fields are visible and correct
            waitForView(withText(title), 5000);
            onView(withText(title)).check(matches(isDisplayed()));
            onView(withText(containsString(senderName))).check(matches(isDisplayed()));
            onView(withText(containsString(receiverName))).check(matches(isDisplayed()));
            onView(withText(description)).check(matches(isDisplayed()));

            // Verify buttons are hidden (Admin Logs are read-only)
            onView(withId(R.id.btn_delete_notification)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btn_accept)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btn_reject)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void testMultipleLogsDisplay() throws Exception {
        // Seed first notification for User 1
        Notification log1 = new Notification(
                null,
                "User 1 Title",
                "sender_id",
                "Organizer 1",
                UserRole.ORGANIZER,
                "user1_id",
                "Recipient 1",
                new Date(System.currentTimeMillis() - 1000),
                "Message 1",
                Notification.TYPE_GENERAL,
                null
        );
        seedNotification(log1);

        // Seed second notification for User 2
        Notification log2 = new Notification(
                null,
                "User 2 Title",
                "sender_id",
                "Organizer 2",
                UserRole.ORGANIZER,
                "user2_id",
                "Recipient 2",
                new Date(),
                "Message 2",
                Notification.TYPE_GENERAL,
                null
        );
        seedNotification(log2);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Admin Panel
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            // Click View Logs
            waitForView(withText("View Logs"), 2000);
            onView(withText("View Logs")).perform(click());

            // Verify both logs are displayed
            waitForView(withText("User 1 Title"), 5000);
            onView(withText("User 1 Title")).check(matches(isDisplayed()));
            onView(withText(containsString("Recipient 1"))).check(matches(isDisplayed()));

            onView(withText("User 2 Title")).check(matches(isDisplayed()));
            onView(withText(containsString("Recipient 2"))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testNoLogsDisplay() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Admin Panel
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            // Click View Logs
            waitForView(withText("View Logs"), 2000);
            onView(withText("View Logs")).perform(click());

            // Verify empty state is shown
            waitForView(withId(R.id.empty_logs_text), 5000);
            onView(withId(R.id.empty_logs_text)).check(matches(isDisplayed()));
            onView(withText("No logs found")).check(matches(isDisplayed()));

            // Verify recycler view is hidden
            onView(withId(R.id.logs_recycler_view)).check(matches(not(isDisplayed())));
        }
    }
}
