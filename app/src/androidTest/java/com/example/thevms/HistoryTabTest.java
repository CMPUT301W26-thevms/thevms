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
import static org.hamcrest.Matchers.not;

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

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
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
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Sets up the database with a specific user role and an event they have joined.
     */
    private void setupTestEnvironment(UserRole role, String status) throws Exception {
        // Seed the user with the given role
        Entrant user = new Entrant(deviceId, "test@example.com", "Test", "User", "1234567890", true, role);
        Tasks.await(user.save(), 10, TimeUnit.SECONDS);

        // Seed an event and add this user to it with specific status
        Organizer org = new Organizer("org_id", "org@test.com", "Event", "Creator", null);
        Event event = Tasks.await(Event.create(
                "Joined Event", "This is a detailed description of the event.", org, null, null,
                new Date(), new Date(System.currentTimeMillis() + 86400000),
                new Date(), new Date(System.currentTimeMillis() + 3600000), false, null, null, false));
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);

        Map<String, Object> regData = new HashMap<>();
        regData.put("entrantId", deviceId);
        regData.put("status", status);
        Tasks.await(testHelper.getDbHandler().updateEntrantStatus(
                String.valueOf(event.getEventId()), deviceId, regData), 10, TimeUnit.SECONDS);

        // Launch the activity after database is prepared
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    // --- Base Test 1: ShowsJoinedEvents (Covers Joined Waitlist) ---

    @Test
    public void testHistoryTab_Entrant_ShowsJoinedEvents() throws Exception {
        setupTestEnvironment(UserRole.ENTRANT, "waiting");
        verifyJoinedEventVisible("Waiting List");
    }

    @Test
    public void testHistoryTab_Organizer_ShowsJoinedEvents() throws Exception {
        setupTestEnvironment(UserRole.ORGANIZER, "waiting");
        verifyJoinedEventVisible("Waiting List");
    }

    @Test
    public void testHistoryTab_Admin_ShowsJoinedEvents() throws Exception {
        setupTestEnvironment(UserRole.ADMIN, "waiting");
        verifyJoinedEventVisible("Waiting List");
    }

    private void verifyJoinedEventVisible(String expectedStatus) throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);
        onView(withText("Joined Event")).check(matches(isDisplayed()));
        onView(withId(R.id.event_status_info)).check(matches(withText(containsString(expectedStatus))));
        onView(withId(R.id.history_title)).check(matches(withText("Event History")));
    }

    // --- Base Test 2: AcceptInvitation (Covers Selected -> Accepted) ---

    @Test
    public void testHistoryTab_Entrant_AcceptInvitation() throws Exception {
        setupTestEnvironment(UserRole.ENTRANT, "selected");
        performAcceptAction();
    }

    @Test
    public void testHistoryTab_Organizer_AcceptInvitation() throws Exception {
        setupTestEnvironment(UserRole.ORGANIZER, "selected");
        performAcceptAction();
    }

    @Test
    public void testHistoryTab_Admin_AcceptInvitation() throws Exception {
        setupTestEnvironment(UserRole.ADMIN, "selected");
        performAcceptAction();
    }

    private void performAcceptAction() throws InterruptedException {
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

    // --- Base Test 3: DeclineInvitation (Covers Selected -> Declined) ---

    @Test
    public void testHistoryTab_Entrant_DeclineInvitation() throws Exception {
        setupTestEnvironment(UserRole.ENTRANT, "selected");
        performDeclineAction();
    }

    @Test
    public void testHistoryTab_Organizer_DeclineInvitation() throws Exception {
        setupTestEnvironment(UserRole.ORGANIZER, "selected");
        performDeclineAction();
    }

    @Test
    public void testHistoryTab_Admin_DeclineInvitation() throws Exception {
        setupTestEnvironment(UserRole.ADMIN, "selected");
        performDeclineAction();
    }

    private void performDeclineAction() throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_decline_event)));

        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("Declined"))))));
    }

    // --- Base Test 4: LeaveEvent (Covers Withdrawal) ---

    @Test
    public void testHistoryTab_Entrant_LeaveEvent() throws Exception {
        setupTestEnvironment(UserRole.ENTRANT, "waiting");
        performLeaveAction();
    }

    @Test
    public void testHistoryTab_Organizer_LeaveEvent() throws Exception {
        setupTestEnvironment(UserRole.ORGANIZER, "waiting");
        performLeaveAction();
    }

    @Test
    public void testHistoryTab_Admin_LeaveEvent() throws Exception {
        setupTestEnvironment(UserRole.ADMIN, "waiting");
        performLeaveAction();
    }

    private void performLeaveAction() throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_leave_event)));

        Thread.sleep(2000);

        onView(withId(R.id.rv_my_events))
                .check(matches(hasDescendant(allOf(withId(R.id.event_status_info), withText(containsString("Not joined"))))));
    }

    // --- Base Test 5: Not Selected Status ---

    @Test
    public void testHistoryTab_Entrant_NotSelected() throws Exception {
        setupTestEnvironment(UserRole.ENTRANT, "rejected");
        verifyJoinedEventVisible("Not Selected");
    }

    @Test
    public void testHistoryTab_Organizer_NotSelected() throws Exception {
        setupTestEnvironment(UserRole.ORGANIZER, "rejected");
        verifyJoinedEventVisible("Not Selected");
    }

    @Test
    public void testHistoryTab_Admin_NotSelected() throws Exception {
        setupTestEnvironment(UserRole.ADMIN, "rejected");
        verifyJoinedEventVisible("Not Selected");
    }

    // --- Base Test 6: ExpandDetails ---

    @Test
    public void testHistoryTab_Entrant_ExpandDetails() throws Exception {
        setupTestEnvironment(UserRole.ENTRANT, "waiting");
        performExpandAction();
    }

    @Test
    public void testHistoryTab_Organizer_ExpandDetails() throws Exception {
        setupTestEnvironment(UserRole.ORGANIZER, "waiting");
        performExpandAction();
    }

    @Test
    public void testHistoryTab_Admin_ExpandDetails() throws Exception {
        setupTestEnvironment(UserRole.ADMIN, "waiting");
        performExpandAction();
    }

    private void performExpandAction() throws InterruptedException {
        onView(withId(R.id.nav_favorites)).perform(click());
        Thread.sleep(2000);

        // Initially description should not be visible (hidden in expandable layout)
        onView(withId(R.id.event_description)).check(matches(not(isDisplayed())));

        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_expand_details)));

        Thread.sleep(1000);

        // After clicking expand, the description should be visible
        onView(withId(R.id.event_description)).check(matches(isDisplayed()));
        onView(withText("This is a detailed description of the event.")).check(matches(isDisplayed()));
    }
}
