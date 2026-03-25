package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.CreateEventFragment;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for Private Event filtering.
 * Verifies that private events created by an organizer are NOT visible in the search listing.
 */
@RunWith(AndroidJUnit4.class)
public class PrivateEventTest {

    private FirestoreTestHelper testHelper;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        Intents.init();
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        deviceId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // Seed an ORGANIZER user
        Entrant organizer = new Entrant(deviceId, "org@example.com", "Event", "Organizer", "123", true, UserRole.ORGANIZER);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testPrivateEventNotVisibleInSearch() throws Exception {
        String privateEventName = "Secret Gathering " + System.currentTimeMillis();
        
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 1. Create a private event
            onView(withId(R.id.nav_add)).perform(click());

            // Bypass date pickers
            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof CreateEventFragment) {
                    Calendar cal = Calendar.getInstance();
                    Date rs = cal.getTime();
                    cal.add(Calendar.HOUR, 1);
                    Date re = cal.getTime();
                    cal.add(Calendar.HOUR, 1);
                    Date es = cal.getTime();
                    cal.add(Calendar.HOUR, 1);
                    Date ee = cal.getTime();
                    ((CreateEventFragment) fragment).setTestingDates(rs, re, es, ee);
                }
            });

            onView(withId(R.id.et_event_name)).perform(replaceText(privateEventName), closeSoftKeyboard());
            onView(withId(R.id.et_event_location)).perform(replaceText("Secret Base"), closeSoftKeyboard());
            onView(withId(R.id.et_event_description)).perform(replaceText("Top secret."), closeSoftKeyboard());

            // Toggle Private Event switch
            onView(withId(R.id.switch_private_event)).perform(scrollTo(), click());

            // Confirm creation
            onView(withId(R.id.btn_confirm)).perform(scrollTo(), click());

            // Wait for DB save (approximate)
            Thread.sleep(2000);

            // 2. Navigate back to Home (SearchFragment) and verify it's NOT displayed
            onView(withId(R.id.nav_home)).perform(click());

            // Search for the specific private event name
            onView(withId(R.id.search_edit_text)).perform(replaceText(privateEventName), closeSoftKeyboard());
            
            // Wait for search result update
            Thread.sleep(2000);

            // Verify result count is 0
            onView(withId(R.id.results_count_text)).check(matches(withText("0 results")));
        }
    }
}
