package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
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
 * UI tests for the Inbox/Notification functionality.
 */
@RunWith(AndroidJUnit4.class)
public class InboxTest {

    private FirestoreTestHelper testHelper;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        // Give the emulator/listeners a moment to settle after clearing
        Thread.sleep(500);

        Context context = ApplicationProvider.getApplicationContext();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed a basic user using abstract model methods
        Entrant entrant = Tasks.await(Entrant.getOrCreate(deviceId), 5, TimeUnit.SECONDS);
        entrant.setEmail("test@example.com");
        entrant.setFirstName("Test");
        entrant.setLastName("User");
        entrant.setPhoneNumber("1234567890");
        Tasks.await(entrant.save(), 5, TimeUnit.SECONDS);
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

    public static ViewAssertion hasItemCount(int expectedCount) {
        return (view, noViewFoundException) -> {
            if (!(view instanceof RecyclerView)) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            if (adapter == null) {
                throw new AssertionError("RecyclerView adapter is null");
            }
            if (adapter.getItemCount() != expectedCount) {
                throw new AssertionError("RecyclerView item count mismatch. Expected: " + expectedCount + ", Actual: " + adapter.getItemCount());
            }
        };
    }

    @Test
    public void testNotificationFieldsDisplayCorrectly() throws Exception {
        Notification notification = new Notification(
                "test_notif_id",
                "Welcome Title",
                "system_id",
                "System Admin",
                UserRole.ADMIN,
                deviceId,
                new Date(),
                "This is a welcome notification message."
        );
        seedNotification(notification);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Inbox
            waitForView(withId(R.id.nav_inbox), 5000);
            onView(withId(R.id.nav_inbox)).perform(click());

            // Check if all fields are visible and correct
            waitForView(withText("Welcome Title"), 5000);
            onView(withText("Welcome Title")).check(matches(isDisplayed()));
            onView(withText(containsString("System Admin"))).check(matches(isDisplayed()));
            onView(withText(containsString("ADMIN"))).check(matches(isDisplayed()));
            onView(withText("This is a welcome notification message.")).check(matches(isDisplayed()));

            // Check for the presence of the delete button
            onView(withId(R.id.btn_delete_notification)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDeleteNotificationConfirmationAndExecution() throws Exception {
        Notification notification = new Notification(
                "test_notif_id",
                "Welcome Title",
                "system_id",
                "System Admin",
                UserRole.ADMIN,
                deviceId,
                new Date(),
                "This is a welcome notification message."
        );
        seedNotification(notification);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Inbox
            waitForView(withId(R.id.nav_inbox), 5000);
            onView(withId(R.id.nav_inbox)).perform(click());

            // Verify notification is there
            waitForView(withText("Welcome Title"), 5000);

            // Click delete button
            onView(withId(R.id.btn_delete_notification)).perform(click());

            // Verify confirmation dialog appears
            waitForView(withText("Delete Notification?"), 2000);
            onView(withText("Delete")).check(matches(isDisplayed()));
            onView(withText("Cancel")).check(matches(isDisplayed()));

            // Test Cancel
            onView(withId(R.id.btn_dialog_cancel)).perform(click());
            onView(withText("Delete Notification?")).check(doesNotExist());

            // Confirm notification is still visible
            onView(withText("Welcome Title")).check(matches(isDisplayed()));

            // Click delete again and Confirm
            onView(withId(R.id.btn_delete_notification)).perform(click());
            waitForView(withText("Delete Notification?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            // Verify empty state is shown
            waitForView(withText("No Notifications"), 5000);
            onView(withId(R.id.empty_inbox_text)).check(matches(isDisplayed()));

            // Verify content is hidden/gone
            onView(withText("Welcome Title")).check(matches(not(isDisplayed())));
            onView(withId(R.id.notifications_recycler_view)).check(hasItemCount(0));
        }
    }

    @Test
    public void testLotteryRejectionFlow() throws Exception {
        String eventId = "test_event_123";
        String eventName = "Test Event";
        
        // 1. Seed 'Lottery Results' notification (Win type to get buttons)
        Notification invite = Notification.createLotteryWin("org_1", "Organizer", deviceId, eventId, eventName);
        seedNotification(invite);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 2. Navigate to Inbox
            waitForView(withId(R.id.nav_inbox), 5000);
            onView(withId(R.id.nav_inbox)).perform(click());

            // 3. Verify notification and buttons are visible
            waitForView(withText("Lottery Results"), 5000);
            onView(withText("Reject")).check(matches(isDisplayed()));
            onView(withText("Accept")).check(matches(isDisplayed()));

            // 4. Click Reject
            onView(withText("Reject")).perform(click());

            // 5. Verify notification is removed from UI
            waitForView(withText("No Notifications"), 5000);
            onView(withText("Lottery Results")).check(matches(not(isDisplayed())));

            // 6. Verify Database state: Entrant status should be 'declined'
            String status = Tasks.await(testHelper.getDbHandler().getEntrantStatus(eventId, deviceId), 5, TimeUnit.SECONDS);
            assertEquals(DatabaseHandler.STATUS_DECLINED, status);
        }
    }

    @Test
    public void testLotteryAcceptFlow() throws Exception {
        String eventId = "test_event_456";
        String eventName = "Accepted Event";
        
        // 1. Seed 'Lottery Results' notification
        Notification invite = Notification.createLotteryWin("org_1", "Organizer", deviceId, eventId, eventName);
        seedNotification(invite);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 2. Navigate to Inbox
            waitForView(withId(R.id.nav_inbox), 5000);
            onView(withId(R.id.nav_inbox)).perform(click());

            // 3. Verify notification and buttons are visible
            waitForView(withText("Lottery Results"), 5000);
            onView(withText("Accept")).check(matches(isDisplayed()));

            // 4. Click Accept
            onView(withText("Accept")).perform(click());

            // 5. Verify notification is removed from UI
            waitForView(withText("No Notifications"), 5000);
            onView(withText("Lottery Results")).check(matches(not(isDisplayed())));

            // 6. Verify Database state: Entrant status should be 'accepted'
            String status = Tasks.await(testHelper.getDbHandler().getEntrantStatus(eventId, deviceId), 5, TimeUnit.SECONDS);
            assertEquals(DatabaseHandler.STATUS_ACCEPTED, status);
        }
    }
}
