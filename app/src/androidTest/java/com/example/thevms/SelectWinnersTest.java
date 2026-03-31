package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.ui.MyEventsActivity;
import com.google.android.gms.tasks.Tasks;

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
public class SelectWinnersTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MyEventsActivity> scenario;
    private long eventId;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        eventId = seedEventForCurrentDevice();
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
    public void organizerCanSelectSpecificNumberOfWinners() throws Exception {
        onView(withId(R.id.btn_run_lottery)).perform(click());
        onView(withText("Select winners")).check(matches(isDisplayed()));
        onView(withId(R.id.et_winner_count)).perform(replaceText("2"), closeSoftKeyboard());
        onView(withText("Select")).perform(click());

        Thread.sleep(2000);

        Map<String, String> statuses = fetchEntrantStatuses(eventId);
        int selected = 0;
        for (String status : statuses.values()) {
            if ("selected".equals(status)) {
                selected++;
            }
        }
        org.junit.Assert.assertEquals(2, selected);
    }

    private long seedEventForCurrentDevice() throws Exception {
        Organizer organizer = new Organizer(deviceId, "org@example.com", "Main", "Organizer", null);
        Event event = Tasks.await(Event.create(
                "Lottery Test Event",
                "Description",
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

        seedUserAndEntrant(event.getEventId(), "winner1", "Alice", "Smith");
        seedUserAndEntrant(event.getEventId(), "winner2", "Bob", "Johnson");
        seedUserAndEntrant(event.getEventId(), "winner3", "Carol", "Davis");
        return event.getEventId();
    }

    private void seedUserAndEntrant(long eventId, String userId, String firstName, String lastName) throws Exception {
        DatabaseHandler db = testHelper.getDbHandler();
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", userId + "@example.com");
        userData.put("role", "ENTRANT");
        Tasks.await(db.saveUser(userId, userData), 10, TimeUnit.SECONDS);

        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("entrantId", userId);
        registrationData.put("status", "waiting");
        registrationData.put("registrationTime", new Date());
        Tasks.await(db.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(userId)
                .set(registrationData), 10, TimeUnit.SECONDS);
    }

    private Map<String, String> fetchEntrantStatuses(long eventId) throws Exception {
        DatabaseHandler db = testHelper.getDbHandler();
        Map<String, String> statuses = new HashMap<>();
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : Tasks.await(
                db.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                        .document(String.valueOf(eventId))
                        .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                        .get(), 10, TimeUnit.SECONDS)) {
            statuses.put(doc.getId(), doc.getString("status"));
        }
        return statuses;
    }
}
