package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * UI tests for MainActivity filtering features.
 * This class tests the SearchFragment UI hosted within MainActivity.
 * It uses @VisibleForTesting helpers in SearchFragment to bypass inconsistent UI interactions
 * like BottomSheet swiping and complex date/time pickers.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() throws Exception {
        Intents.init();

        // 1. Initialize the test helper. Its constructor calls dbHandler.useEmulator().
        // We do this BEFORE launching the activity to ensure the activity uses the emulator.
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        // 2. Seed dummy events: "Test Event 1" and "Test Event 2"
        testHelper.seedDummyEvents(2);

        // 3. Launch the activity manually after the environment is ready.
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        Intents.release();
    }

    /**
     * Test filtering events by name.
     * Uses expandBottomSheet() helper to ensure visibility without flaky swipes.
     */
    @Test
    public void testFilterByName() throws InterruptedException {
        // Initially, we expect 2 results
        onView(withId(R.id.results_count_text)).check(matches(withText(containsString("2 results"))));

        // Type "Test Event 1" in the search EditText
        onView(withId(R.id.search_edit_text)).perform(typeText("Test Event 1"), closeSoftKeyboard());

        // Use the helper to expand the bottom sheet programmatically
        scenario.onActivity(activity -> {
            SearchFragment fragment = (SearchFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (fragment != null) {
                fragment.expandBottomSheet();
            }
        });

        // Wait for the expansion to complete
        Thread.sleep(1000);

        // Verify that only 1 result is displayed
        onView(withId(R.id.results_count_text)).check(matches(withText(containsString("1 results"))));

        // Verify the event name is displayed in the list.
        onView(allOf(withId(R.id.event_name), withText("Test Event 1"))).check(matches(isDisplayed()));
    }

    /**
     * Test filtering using the @VisibleForTesting helper.
     * Bypasses the complex TimePickerDialog.
     */
    @Test
    public void testFilterByTimeRangeHelper() throws InterruptedException {
        // Set a broad time range that includes the current time (when seeded events are scheduled)
        scenario.onActivity(activity -> {
            SearchFragment fragment = (SearchFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (fragment != null) {
                // Range: 00:00 to 23:59
                fragment.setTestingFilters(null, null, 0, 0, 23, 59);
                fragment.expandBottomSheet();
            }
        });

        Thread.sleep(1000);

        // Verify we still see both results as they fall within the range
        onView(withId(R.id.results_count_text)).check(matches(withText(containsString("2 results"))));

        // Verify the "Clear" filters button appeared
        onView(withId(R.id.clear_filters_btn)).check(matches(isDisplayed()));
    }

    /**
     * Test clearing the search filter.
     */
    @Test
    public void testClearSearch() throws InterruptedException {
        // Type something that matches nothing
        onView(withId(R.id.search_edit_text)).perform(typeText("NonExistentEvent"), closeSoftKeyboard());

        Thread.sleep(1000);
        onView(withId(R.id.results_count_text)).check(matches(withText(containsString("0 results"))));

        // Click on the clear search icon (the 'X')
        onView(withId(R.id.clear_search)).perform(click());

        Thread.sleep(1000);
        // Verify that we are back to seeing all 2 results
        onView(withId(R.id.results_count_text)).check(matches(withText(containsString("2 results"))));
    }

    /**
     * Test visibility of filter buttons.
     */
    @Test
    public void testFilterButtonsVisible() {
        onView(withId(R.id.filter_date_range_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.filter_time_range_btn)).check(matches(isDisplayed()));
    }

    /**
     * Test joining an event and verifying the count updates from 0 to 1.
     */
    @Test
    public void testJoinEvent_UpdatesCount() throws Exception {
        // Swipe up to ensure bottom sheet is visible
        expandBottomSheetHelper();
        Thread.sleep(1000);

        // Verify initial state: 0 people joined
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("0"))))));

        // Click the Join Event button on the first card
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_join_event)));

        // Wait for Firestore update and UI refresh
        Thread.sleep(2000);

        // Verify the count updated to 1 in the DB
        Long dbCount = Tasks.await(testHelper.getDbHandler().getEntrantCount("1"), 10, TimeUnit.SECONDS);
        assertEquals(1L, (long) dbCount);

        // Verify the count updated to 1
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("1"))))));
    }

    /**
     * Test joining an event that already has waitees.
     */
    @Test
    public void testJoinEvent_WithExistingWaitees() throws Exception {
        // Seed 3 entrants for "Test Event 1" (which should have ID 1 after clearDatabase)
        testHelper.seedEntrants(1, 3);

        Thread.sleep(1000);

        // Relaunch to ensure fresh adapter state
        scenario.close();
        scenario = ActivityScenario.launch(MainActivity.class);

        // Swipe up to ensure bottom sheet is visible
        expandBottomSheetHelper();
        Thread.sleep(1000);

        // Verify initial state: 3 people joined
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("3"))))));

        // Click Join
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_join_event)));

        // Wait for increment
        Thread.sleep(2000);

        // Verify the count updated to 4 in the DB
        Long dbCount = Tasks.await(testHelper.getDbHandler().getEntrantCount("1"), 10, TimeUnit.SECONDS);
        assertEquals(4L, (long) dbCount);

        // Verify count is now 4
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("4"))))));
    }

    /**
     * Helper to programmatically expand the SearchFragment's bottom sheet.
     */
    private void expandBottomSheetHelper() {
        scenario.onActivity(activity -> {
            SearchFragment fragment = (SearchFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (fragment != null) {
                fragment.expandBottomSheet();
            }
        });
    }

    /**
     * Helper action to click a child view with a specific ID within a RecyclerView item.
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }

    /**
     * Test leaving an event and verifying the count updates from 0 to 1 then 0.
     */
    @Test
    public void testLeaveEvent_UpdatesCount() throws Exception {
        // Get device ID
        String realDeviceId = android.provider.Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        // Seed 1 entrants for "Test Event 1"
        testHelper.seedSpecificEntrant(1, realDeviceId);
        Thread.sleep(1000);

        // Relaunch to ensure fresh adapter state
        scenario.close();
        scenario = ActivityScenario.launch(MainActivity.class);

        // Swipe up to ensure bottom sheet is visible
        expandBottomSheetHelper();
        Thread.sleep(1000);

        // Verify initial state: 1 people joined and "Leave" button is visible
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("1"))))));
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.btn_leave_event), isDisplayed()))));

        // Click Leave
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_leave_event)));

        // Wait for increment
        Thread.sleep(2000);

        // Verify the count updated to 0 in the DB
        Long dbCount = Tasks.await(testHelper.getDbHandler().getEntrantCount("1"), 10, TimeUnit.SECONDS);
        assertEquals(0L, (long) dbCount);

        // Verify count is now 0 and "Join" button is visible
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("0"))))));
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.btn_join_event), isDisplayed()))));
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(withId(R.id.btn_leave_event), not(isDisplayed())))));
    }

    /**
     * Test if user is within the event's required location.
     */
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    );

    @Before
    public void setUpMockLocation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set " + context.getPackageName() + " android:mock_location allow");

        testHelper = new FirestoreTestHelper();
    }

    @Test
    public void testJoinEvent_withinLocation() throws Exception {
        // Seed an event
        String eventId = "1";
        testHelper.setMockLocation(53.52, -113.52);
        Thread.sleep(2000);


        // Relaunch to ensure fresh adapter state
        scenario = ActivityScenario.launch(MainActivity.class);
        expandBottomSheetHelper();
        Thread.sleep(1000);

        // Click Join button
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                        clickChildViewWithId(R.id.btn_join_event)));

        Thread.sleep(5000);

        // Entrant should be in the waitlist, Join button should disappear
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(
                        withId(R.id.btn_leave_event),
                        isDisplayed()
                ))));

        // Check the count in the database
        Long count = Tasks.await(testHelper.getDbHandler().getEntrantCount(eventId), 5, TimeUnit.SECONDS);
        assertEquals(1L, (long) count);
    }

    @Test
    public void testJoinEvent_notWithinLocation() throws Exception {
        // Seed an event
        String eventId = "1";
        testHelper.setMockLocation(55, 66);

        // Relaunch to ensure fresh adapter state
        scenario = ActivityScenario.launch(MainActivity.class);
        expandBottomSheetHelper();
        Thread.sleep(1000);

        // Click Join button
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                        clickChildViewWithId(R.id.btn_join_event)));

        // Entrant should be in the waitlist, Join button should not disappear
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(allOf(
                        withId(R.id.btn_join_event),
                        isDisplayed()
                ))));

        // Check the count in the database
        Long count = Tasks.await(testHelper.getDbHandler().getEntrantCount(String.valueOf(eventId)), 5, TimeUnit.SECONDS);
        assertEquals(0L, (long) count);
    }
}
