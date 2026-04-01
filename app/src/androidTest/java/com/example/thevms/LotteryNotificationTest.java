package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.model.Notification;
import com.example.thevms.model.Organizer;
import com.example.thevms.ui.MyEventsActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented test to verify that notifications are sent to winners and losers 
 * after running a lottery in OrganizerEventAdapter.
 */
@RunWith(AndroidJUnit4.class)
public class LotteryNotificationTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MyEventsActivity> scenario;
    private long eventId;
    private String deviceId;
    private DatabaseHandler dbHandler;

    private static final String WINNER_ID = "winner_entrant";
    private static final String LOSER_ID = "loser_entrant";

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();
        dbHandler = testHelper.getDbHandler();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed event and entrants
        eventId = seedEventWithEntrants();
        
        scenario = ActivityScenario.launch(MyEventsActivity.class);
        // Wait for UI to load
        Thread.sleep(3000);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void testLotterySendsNotifications() throws Exception {
        // 1. Click 'Run Lottery' button
        onView(withId(R.id.btn_run_lottery)).perform(click());

        // 2. Enter '1' for winner count (we have 2 entrants, so 1 winner, 1 loser)
        onView(withId(R.id.et_winner_count)).perform(replaceText("1"), closeSoftKeyboard());
        
        // 3. Confirm selection
        onView(withText("Select")).perform(click());

        // 4. Wait for processing and Firestore sync
        Thread.sleep(5000);

        // 5. Verify Notifications collection in Firestore
        QuerySnapshot notificationSnapshot = Tasks.await(
                dbHandler.getDb().collection(DatabaseHandler.COLLECTION_NOTIFICATIONS).get(),
                10, TimeUnit.SECONDS
        );

        // Expect exactly 2 notifications (1 for winner, 1 for loser)
        assertEquals("Should have 2 notifications in total", 2, notificationSnapshot.size());

        boolean foundWinnerNotification = false;
        boolean foundLoserNotification = false;

        for (QueryDocumentSnapshot doc : notificationSnapshot) {
            String receiverId = doc.getString("receiverId");
            String type = doc.getString("type");
            String title = doc.getString("title");

            if (WINNER_ID.equals(receiverId)) {
                foundWinnerNotification = true;
                assertEquals("Winner notification type should be invite", Notification.TYPE_INVITE, type);
                assertTrue("Winner notification title should contain Lottery", title.contains("Lottery"));
            } else if (LOSER_ID.equals(receiverId)) {
                foundLoserNotification = true;
                assertEquals("Loser notification type should be general", Notification.TYPE_GENERAL, type);
                assertTrue("Loser notification title should contain Lottery", title.contains("Lottery"));
            }
        }

        assertTrue("Should have sent a notification to the winner", foundWinnerNotification);
        assertTrue("Should have sent a notification to the loser", foundLoserNotification);
    }

    private long seedEventWithEntrants() throws Exception {
        // Create Organizer (Current Device)
        Map<String, Object> orgData = new HashMap<>();
        orgData.put("firstName", "Admin");
        orgData.put("lastName", "Organizer");
        orgData.put("role", "ORGANIZER");
        Tasks.await(dbHandler.saveUser(deviceId, orgData), 10, TimeUnit.SECONDS);

        Organizer organizer = new Organizer(deviceId, "org@test.com", "Admin", "Organizer", null);
        
        // Create Event
        Event event = Tasks.await(Event.create(
                "Notification Test Event",
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

        // Seed 2 Entrants in 'waiting' status
        seedEntrant(event.getEventId(), WINNER_ID, "Winner", "User");
        seedEntrant(event.getEventId(), LOSER_ID, "Loser", "User");

        return event.getEventId();
    }

    private void seedEntrant(long eventId, String userId, String first, String last) throws Exception {
        // Save User Profile
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", first);
        userData.put("lastName", last);
        userData.put("role", "ENTRANT");
        Tasks.await(dbHandler.saveUser(userId, userData), 10, TimeUnit.SECONDS);

        // Register for Event as 'waiting'
        Map<String, Object> reg = new HashMap<>();
        reg.put("entrantId", userId);
        reg.put("status", DatabaseHandler.STATUS_WAITING);
        reg.put("registrationTime", new Date());
        
        Tasks.await(dbHandler.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(userId)
                .set(reg), 10, TimeUnit.SECONDS);
    }
}
