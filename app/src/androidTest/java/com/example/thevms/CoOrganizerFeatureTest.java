package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CoOrganizerFeatureTest {

    private static final String ENTRANT_ID = "entrant-test-001";
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

        eventId = seedOrganizerEvent();
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
    public void organizerAssignsCoOrganizer_andEntrantCannotRejoin() throws Exception {
        onView(withId(R.id.rv_my_events))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_assign_co_organizer)));

        Thread.sleep(2000);

        FirebaseFirestore db = helper.getDbHandler().getDb();
        DocumentSnapshot entrantDoc = Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(ENTRANT_ID)
                .get(), 10, TimeUnit.SECONDS);
        assertEquals(DatabaseHandler.STATUS_CO_ORGANIZER, entrantDoc.getString("status"));

        QuerySnapshot notifSnapshot = Tasks.await(db.collection(DatabaseHandler.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverId", ENTRANT_ID)
                .whereEqualTo("eventId", String.valueOf(eventId))
                .get(), 10, TimeUnit.SECONDS);
        assertFalse(notifSnapshot.isEmpty());

        Event event = Event.fromDoc(Tasks.await(db.collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId)).get(), 10, TimeUnit.SECONDS));
        Entrant entrant = new Entrant(ENTRANT_ID, "co@example.com", "Co", "Org", null, true, UserRole.ENTRANT);
        try {
            Tasks.await(event.addEntrant(entrant, null));
            fail("Expected co-organizer join attempt to fail");
        } catch (ExecutionException ex) {
            assertEquals(IllegalStateException.class, ex.getCause().getClass());
        }
    }

    private long seedOrganizerEvent() throws Exception {
        Organizer organizer = new Organizer(organizerDeviceId, "org@example.com", "Org", "Owner", null);
        Event event = Tasks.await(Event.create(
                "CoOrg Test Event",
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

        seedEntrant(event.getEventId(), ENTRANT_ID, "Alice", "Cooper");
        return event.getEventId();
    }

    private void seedEntrant(long eventId, String userId, String firstName, String lastName) throws Exception {
        DatabaseHandler db = helper.getDbHandler();
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", userId + "@example.com");
        userData.put("role", "ENTRANT");
        Tasks.await(db.saveUser(userId, userData), 10, TimeUnit.SECONDS);

        Map<String, Object> registration = new HashMap<>();
        registration.put("entrantId", userId);
        registration.put("status", DatabaseHandler.STATUS_WAITING);
        registration.put("registrationTime", new Date());
        Tasks.await(db.getDb().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(String.valueOf(eventId))
                .collection(DatabaseHandler.COLLECTION_ENTRANTS)
                .document(userId)
                .set(registration), 10, TimeUnit.SECONDS);
    }

    private static ViewAction clickChildViewWithId(int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
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
