package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;
import com.google.android.gms.tasks.Tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests to verify that waitlist limiting works correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistLimitUITest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();
        
        // Seed 1 event
        testHelper.seedDummyEvents(1);
        
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set " + context.getPackageName() + " android:mock_location allow");
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test case: Waitlist is 3/4. User should be able to see the join button enabled.
     */
    @Test
    public void testWaitlistNotFull() throws Exception {
        // Set maxWaitlist to 4 for event "1"
        Map<String, Object> updates = new HashMap<>();
        updates.put("maxWaitlist", 4);
        Tasks.await(testHelper.getDbHandler().getDb()
                .collection(DatabaseHandler.COLLECTION_EVENTS)
                .document("1")
                .update(updates), 5, TimeUnit.SECONDS);

        // Seed 3 entrants
        testHelper.seedEntrants(1, 3);

        scenario = ActivityScenario.launch(MainActivity.class);
        expandBottomSheetHelper();
        Thread.sleep(2000);

        // Verify waitlist text shows 3/4
        onView(withId(R.id.event_waitlist_info))
                .check(matches(allOf(isDisplayed(), withText(containsString("Waitlist: 3/4")))));

        // Verify Join button is enabled and says "Join"
        onView(withId(R.id.events_recycler_view))
                .check(matches(MainActivityTest.hasDescendant(allOf(
                        withId(R.id.btn_join_event),
                        isDisplayed(),
                        isEnabled(),
                        withText("Join")
                ))));
    }

    /**
     * Test case: Waitlist is 4/4. Join button should be greyed out and say "Waitlist Full".
     */
    @Test
    public void testWaitlistFull() throws Exception {
        // Set maxWaitlist to 4 for event "1"
        Map<String, Object> updates = new HashMap<>();
        updates.put("maxWaitlist", 4);
        Tasks.await(testHelper.getDbHandler().getDb()
                .collection(DatabaseHandler.COLLECTION_EVENTS)
                .document("1")
                .update(updates), 5, TimeUnit.SECONDS);

        // Seed 4 entrants
        testHelper.seedEntrants(1, 4);

        scenario = ActivityScenario.launch(MainActivity.class);
        expandBottomSheetHelper();
        Thread.sleep(2000);

        // Verify waitlist text shows 4/4
        onView(withId(R.id.event_waitlist_info))
                .check(matches(allOf(isDisplayed(), withText(containsString("Waitlist: 4/4")))));

        // Verify Join button is disabled and says "Waitlist Full"
        onView(withId(R.id.events_recycler_view))
                .check(matches(MainActivityTest.hasDescendant(allOf(
                        withId(R.id.btn_join_event),
                        isDisplayed(),
                        not(isEnabled()),
                        withText("Waitlist Full")
                ))));
    }

    private void expandBottomSheetHelper() {
        scenario.onActivity(activity -> {
            SearchFragment fragment = (SearchFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (fragment != null) {
                fragment.expandBottomSheet();
            }
        });
    }
}
