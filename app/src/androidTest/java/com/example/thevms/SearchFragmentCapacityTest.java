package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented test for SearchFragment capacity filtering.
 */
@RunWith(AndroidJUnit4.class)
public class SearchFragmentCapacityTest {

    private FirestoreTestHelper testHelper;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        deviceId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Create a regular user
        Entrant user = new Entrant(deviceId, "test@example.com", "First", "Last", "123", true, UserRole.ENTRANT);
        Tasks.await(user.save(), 5, TimeUnit.SECONDS);

        // Seed events with different capacities
        Organizer organizer = new Organizer(deviceId, "org@example.com", "Org", "Name", "123");
        
        Calendar cal = Calendar.getInstance();
        Date regStart = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date regEnd = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date eventStart = cal.getTime();
        cal.add(Calendar.HOUR, 2);
        Date eventEnd = cal.getTime();

        // Event 1: Capacity 50
        Event event50 = Tasks.await(Event.create("Event 50", "Desc", organizer, "Loc", null, regStart, regEnd, eventStart, eventEnd, false, 0.0, null), 5, TimeUnit.SECONDS);
        event50.setMaxAttendees(50);
        Tasks.await(event50.save(), 5, TimeUnit.SECONDS);

        // Event 2: Capacity 100
        Event event100 = Tasks.await(Event.create("Event 100", "Desc", organizer, "Loc", null, regStart, regEnd, eventStart, eventEnd, false, 0.0, null), 5, TimeUnit.SECONDS);
        event100.setMaxAttendees(100);
        Tasks.await(event100.save(), 5, TimeUnit.SECONDS);
        
        // Event 3: Capacity 54 (Within range of 50±5)
        Event event54 = Tasks.await(Event.create("Event 54", "Desc", organizer, "Loc", null, regStart, regEnd, eventStart, eventEnd, false, 0.0, null), 5, TimeUnit.SECONDS);
        event54.setMaxAttendees(54);
        Tasks.await(event54.save(), 5, TimeUnit.SECONDS);
    }

    @Test
    public void testCapacityFilter_RangeFiltering() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Wait for events to load
            Thread.sleep(2000);

            // Use testing helper to apply capacity filter of 50
            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof SearchFragment) {
                    ((SearchFragment) fragment).setTestingFilters(null, null, null, null, null, null, 50);
                }
            });

            // Wait for filter to apply
            Thread.sleep(1000);

            // Verify results count (should be 2: "Event 50" and "Event 54")
            onView(withId(R.id.results_count_text)).check(matches(withText(containsString("2 results"))));
            
            // Expand bottom sheet to see events
            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof SearchFragment) {
                    ((SearchFragment) fragment).expandBottomSheet();
                }
            });

            // Wait for bottom sheet animation
            Thread.sleep(1000);

            // Verify that both expected events are present in the RecyclerView
            // This method checks if the list HAS a descendant with the given text, 
            // which is less sensitive to animations and exact visibility than scrollTo
            onView(withId(R.id.events_recycler_view))
                    .check(matches(hasDescendant(withText("Event 50"))));
            
            onView(withId(R.id.events_recycler_view))
                    .check(matches(hasDescendant(withText("Event 54"))));

            // Optional: verify that the one outside the range is NOT present
            // onView(withId(R.id.events_recycler_view))
            //         .check(matches(not(hasDescendant(withText("Event 100")))));
        }
    }
}
