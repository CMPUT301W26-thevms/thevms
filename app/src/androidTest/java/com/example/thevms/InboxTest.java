package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

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

        Context context = ApplicationProvider.getApplicationContext();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed a basic user
        Entrant user = new Entrant(deviceId, "test@example.com", "Test", "User", "1234567890");
        Tasks.await(user.save(), 5, TimeUnit.SECONDS);

        // Seed a notification for this user
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
        Tasks.await(testHelper.getDbHandler().sendNotification(notification), 5, TimeUnit.SECONDS);
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
    public void testNotificationFieldsDisplayCorrectly() throws InterruptedException {
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
}
