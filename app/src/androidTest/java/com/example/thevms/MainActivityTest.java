package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
}
