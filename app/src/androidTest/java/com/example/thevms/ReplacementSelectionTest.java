package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.ui.MyEventsActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
public class ReplacementSelectionTest {

    private static final String DECLINED_ONE_ID = "declined-001";
    private static final String DECLINED_TWO_ID = "declined-002";
    private static final String WAITING_ONE_ID = "waiting-101";
    private static final String WAITING_TWO_ID = "waiting-102";

    private FirestoreTestHelper helper;
    private ActivityScenario<MyEventsActivity> scenario;
    private long eventId;
    private String organizerDeviceId;

    @Before
    public void setUp() throws Exception {
        helper = new FirestoreTestHelper();
        helper.clearDatabase();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        organizerDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        eventId = seedReplacementScenario();
        scenario = ActivityScenario.launch(MyEventsActivity.class);
        Thread.sleep(2000);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void replacementDialog_displaysWaitingEntrantsFromPool() throws Exception {
        openReplacementDialog();

        onView(withId(R.id.tv_replacement_empty))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.rv_declined_users)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_notify_declined))
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(containsString("Notify")))));

        onView(allOf(withId(R.id.tv_replacement_name), withText("Waiting One")))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.cb_replacement_select), hasSibling(withText("Waiting One"))))
                .check(matches(allOf(isDisplayed(), isNotChecked())));

        onView(allOf(withId(R.id.tv_replacement_name), withText("Waiting Two")))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.cb_replacement_select), hasSibling(withText("Waiting Two"))))
                .check(matches(allOf(isDisplayed(), isNotChecked())));
    }

    @Test
    public void notifyButton_updatesOnlyCheckedEntrants() throws Exception {
        openReplacementDialog();

        ViewInteraction firstCheckbox = onView(allOf(withId(R.id.cb_replacement_select), hasSibling(withText("Waiting One"))));
        ViewInteraction secondCheckbox = onView(allOf(withId(R.id.cb_replacement_select), hasSibling(withText("Waiting Two"))));

        firstCheckbox.perform(click());
        firstCheckbox.check(matches(isChecked()));

        secondCheckbox.perform(click());
        secondCheckbox.check(matches(isChecked()));

        // Organizer can change their mind and only alert one entrant
        secondCheckbox.perform(click());
        secondCheckbox.check(matches(isNotChecked()));

        onView(withId(R.id.btn_notify_declined)).perform(click());
        Thread.sleep(2000);

        FirebaseFirestore db = helper.getDbHandler().getDb();

        DocumentSnapshot waitingOneDoc = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(WAITING_ONE_ID)
                .get(), 10, TimeUnit.SECONDS);
        assertEquals(DatabaseHandler.STATUS_SELECTED, waitingOneDoc.getString("status"));

        DocumentSnapshot waitingTwoDoc = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(WAITING_TWO_ID)
                .get(), 10, TimeUnit.SECONDS);
        assertEquals(DatabaseHandler.STATUS_WAITING, waitingTwoDoc.getString("status"));

        DocumentSnapshot declinedDoc = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(DECLINED_ONE_ID)
                .get(), 10, TimeUnit.SECONDS);
        assertEquals(DatabaseHandler.STATUS_DECLINED, declinedDoc.getString("status"));

        QuerySnapshot notifiedFirst = Tasks.await(db.collection(DatabaseHandler.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverId", WAITING_ONE_ID)
                .whereEqualTo("eventId", String.valueOf(eventId))
                .get(), 10, TimeUnit.SECONDS);
        assertEquals(1, notifiedFirst.size());

        QuerySnapshot notifiedSecond = Tasks.await(db.collection(DatabaseHandler.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverId", WAITING_TWO_ID)
                .whereEqualTo("eventId", String.valueOf(eventId))
                .get(), 10, TimeUnit.SECONDS);
        assertTrue(notifiedSecond.isEmpty());
    }

    private long seedReplacementScenario() throws Exception {
        Organizer organizer = new Organizer(organizerDeviceId, "org@example.com", "Org", "Owner", null);
        Event event = Tasks.await(Event.create(
                "Replacement Drill",
                "Organizer replacement scenario",
                organizer,
                null,
                null,
                new Date(),
                new Date(System.currentTimeMillis() + 86400000),
                new Date(),
                new Date(System.currentTimeMillis() + 3600000),
                false,
                0.0,
                null,
                false
        ), 10, TimeUnit.SECONDS);
        Tasks.await(event.save(), 10, TimeUnit.SECONDS);

        seedEntrant(event.getEventId(), DECLINED_ONE_ID, "Declined", "One", DatabaseHandler.STATUS_DECLINED);
        seedEntrant(event.getEventId(), DECLINED_TWO_ID, "Cancelled", "User", DatabaseHandler.STATUS_CANCELLED);
        seedEntrant(event.getEventId(), WAITING_ONE_ID, "Waiting", "One", DatabaseHandler.STATUS_WAITING);
        seedEntrant(event.getEventId(), WAITING_TWO_ID, "Waiting", "Two", DatabaseHandler.STATUS_WAITING);
        return event.getEventId();
    }

    private void seedEntrant(long eventId, String userId, String firstName, String lastName, String status) throws Exception {
        DatabaseHandler db = helper.getDbHandler();

        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", userId + "@example.com");
        userData.put("role", "ENTRANT");
        Tasks.await(db.saveUser(userId, userData), 10, TimeUnit.SECONDS);

        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("entrantId", userId);
        registrationData.put("status", status);
        registrationData.put("registrationTime", new Date());
        registrationData.put("entrantLat", 53.5200);
        registrationData.put("entrantLng", -113.5000);

        Tasks.await(db.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(userId)
                .set(registrationData), 10, TimeUnit.SECONDS);
    }

    private void openReplacementDialog() throws InterruptedException {
        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_pick_replacements)));
        Thread.sleep(1500);
    }

    private static ViewAction clickChildViewWithId(int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return is(View.class);
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View childView = view.findViewById(id);
                if (childView != null) {
                    childView.performClick();
                }
            }
        };
    }
}
