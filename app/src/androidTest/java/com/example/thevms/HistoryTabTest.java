package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryTabTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MainActivity> scenario;
    private String deviceId;

    /**
     * Helper action to click a child view with a specific ID within a RecyclerView item.
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() { return null; }
            @Override
            public String getDescription() { return "Click on a child view with specified id."; }
            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) v.performClick();
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        seedTestData();

        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    private void seedTestData() throws Exception {
        Organizer org = new Organizer("org_id", "org@test.com", "Event", "Creator", null);
        
        Event event = Tasks.await(Event.create(
                "Joined Event", "Desc", org, null, null,
                new Date(), new Date(System.currentTimeMillis() + 86400000),
                new Date(), new Date(System.currentTimeMillis() + 3600000)
        ), 10, TimeUnit.SECONDS);
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);

        Map<String, Object> regData = new HashMap<>();
        regData.put("entrantId", deviceId);
        regData.put("status", "selected");
        Tasks.await(testHelper.getDbHandler().updateEntrantStatus(
                String.valueOf(event.getEventId()), deviceId, regData), 10, TimeUnit.SECONDS);
    }

    @Test
    public void testHistoryTab_ShowsJoinedEvents() throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);
        onView(withText("Joined Event")).check(matches(isDisplayed()));
    }

    @Test
    public void testHistoryTab_AcceptInvitation() throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .check(matches(hasDescendant(allOf(withId(R.id.btn_accept_event), isDisplayed()))));

        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_accept_event)));

        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("Accepted"))))));
    }

    @Test
    public void testHistoryTab_DeclineInvitation() throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_decline_event)));

        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("Declined"))))));
    }
}
