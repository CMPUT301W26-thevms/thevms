package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
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
 * UI tests for Admin Event management.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class AdminEventTest {

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

        // Seed an Organizer
        Organizer organizer = new Organizer("org1", "bob@example.com", "Bob", "Jones", null);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);

        // Seed some Events
        Date pastStart = new Date(System.currentTimeMillis() - 3600000);
        Date futureEnd = new Date(System.currentTimeMillis() + 3600000);

        Event event1 = Tasks.await(Event.create("Event One", "Description One", organizer, null, null, pastStart, futureEnd, new Date(), new Date()), 5, TimeUnit.SECONDS);
        Tasks.await(event1.save(), 5, TimeUnit.SECONDS);

        Event event2 = Tasks.await(Event.create("Event Two", "Description Two", organizer, null, null, pastStart, futureEnd, new Date(), new Date()), 5, TimeUnit.SECONDS);
        Tasks.await(event2.save(), 5, TimeUnit.SECONDS);
    }

    /**
     * Helper to wait for a view to appear.
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
     * Helper to wait for a view to disappear.
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

    /**
     * Custom ViewAssertion to check the item count of a RecyclerView.
     */
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
    public void testAdminCanNavigateToManageEvents() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Wait for the admin gear icon and click it
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            // Click on "Manage Event" in the admin drawer
            waitForView(withText("Manage Event"), 2000);
            onView(withText("Manage Event")).perform(click());

            // Wait for events to load and the loading spinner to disappear
            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            // Verify the title of the Manage Events screen
            onView(withId(R.id.admin_events_title_text)).check(matches(withText("Manage Events")));

            // Verify that the seeded events are displayed
            // We use scrollTo to handle cases where the item might be off-screen
            onView(withId(R.id.events_recycler_view))
                    .perform(RecyclerViewActions.scrollTo(hasDescendant(withText("Event One"))));
            onView(withText("Event One")).check(matches(isDisplayed()));

            onView(withId(R.id.events_recycler_view))
                    .perform(RecyclerViewActions.scrollTo(hasDescendant(withText("Event Two"))));
            onView(withText("Event Two")).check(matches(isDisplayed()));

            // Verify the item count in the RecyclerView
            onView(withId(R.id.events_recycler_view)).check(hasItemCount(2));
        }
    }
}
