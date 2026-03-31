package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for Admin Profile management.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class AdminProfilesTest {

    private FirestoreTestHelper testHelper;
    private String adminDeviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = ApplicationProvider.getApplicationContext();
        adminDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed an admin user so the gear icon is visible
        Entrant admin = new Entrant(adminDeviceId, "admin@example.com", "Admin", "User", "1234567890", true, UserRole.ADMIN);
        Tasks.await(admin.save(), 5, TimeUnit.SECONDS);

        // Seed a test user (Entrant)
        Entrant user1 = new Entrant("user1", "alice@example.com", "Alice", "Smith", "1112223333");
        Tasks.await(user1.save(), 5, TimeUnit.SECONDS);

        // Seed an Organizer
        Entrant organizer = new Entrant("org1", "bob@example.com", "Bob", "Jones", "4445556666", true, UserRole.ORGANIZER);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);
    }

    /**
     * Helper to wait for a view to appear, reducing flakiness from async operations like role checks.
     */
    private void waitForView(Matcher<View> matcher, int timeoutMs) throws InterruptedException {
        long timeoutMillis = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timeoutMillis) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (Exception | AssertionError e) {
                Thread.sleep(200);
            }
        }
        onView(matcher).check(matches(isDisplayed()));
    }

    /**
     * Helper to wait for a view to disappear (e.g., loading spinner).
     */
    private void waitForViewToDisappear(Matcher<View> matcher, int timeoutMs) throws InterruptedException {
        long timeoutMillis = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timeoutMillis) {
            try {
                onView(matcher).check(matches(not(isDisplayed())));
                return;
            } catch (Exception | AssertionError e) {
                Thread.sleep(200);
            }
        }
    }

    /**
     * Custom ViewAction to click a child view with a specific ID within a RecyclerView item.
     */
    public static ViewAction clickChildViewWithId(final int id) {
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
                View v = view.findViewById(id);
                if (v != null) {
                    v.performClick();
                }
            }
        };
    }

    /**
     * Custom ViewAssertion to check the item count of a RecyclerView.
     */
    public static ViewAssertion hasItemCount(int expectedCount) {
        return (view, noViewFoundException) -> {
            if (!(view instanceof RecyclerView)) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            if (adapter == null) {
                throw new AssertionError("RecyclerView adapter is null");
            }
            if (adapter.getItemCount() != expectedCount) {
                throw new AssertionError("RecyclerView item count mismatch. Expected: " + expectedCount + ", Actual: " + adapter.getItemCount());
            }
        };
    }

    @Test
    public void testAdminCanNavigateToProfiles() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Profiles"), 2000);
            onView(withText("Manage Profiles")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.admin_profiles_title_text)).check(matches(withText("Manage Profiles")));
            onView(withText("Alice Smith")).check(matches(isDisplayed()));
            onView(withText("Bob Jones")).check(matches(isDisplayed()));

            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(3));
        }
    }

    @Test
    public void testAdminCanNavigateToOrganizers() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Organizers"), 2000);
            onView(withText("Manage Organizers")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.admin_profiles_title_text)).check(matches(withText("Manage Organizers")));

            onView(withText("Bob Jones")).check(matches(isDisplayed()));
            onView(withText("Alice Smith")).check(doesNotExist());

            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(1));
        }
    }

    @Test
    public void testDeleteProfileConfirmationModal() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Profiles"), 2000);
            onView(withText("Manage Profiles")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(3));

            // Specifically target Alice Smith's card to avoid clicking on Admin User (self)
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Alice Smith")), clickChildViewWithId(R.id.btn_delete_profile)));

            waitForView(withText("Delete Profile?"), 2000);
            onView(withText("Delete")).check(matches(isDisplayed()));
            onView(withText("Cancel")).check(matches(isDisplayed()));

            onView(withId(R.id.btn_dialog_cancel)).perform(click());
            onView(withText("Delete Profile?")).check(doesNotExist());

            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(3));
        }
    }

    @Test
    public void testDeleteProfileSucceeds() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Profiles"), 2000);
            onView(withText("Manage Profiles")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(3));

            // Specifically delete Alice Smith (Entrant)
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Alice Smith")), clickChildViewWithId(R.id.btn_delete_profile)));

            waitForView(withText("Delete Profile?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            // Wait for deletion cascade and refresh spinner
            waitForViewToDisappear(withId(R.id.loading_spinner), 10000);

            // Verify count decreased to 2 and Alice is gone
            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(2));
            onView(withText("Alice Smith")).check(doesNotExist());
        }
    }

    @Test
    public void testDeleteOrganizerSucceeds() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Organizers"), 2000);
            onView(withText("Manage Organizers")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            // Confirm 1 organizer (Bob)
            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(1));

            // Delete Bob
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Bob Jones")), clickChildViewWithId(R.id.btn_delete_profile)));

            waitForView(withText("Delete Profile?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            // Wait for deletion cascade and refresh spinner
            waitForViewToDisappear(withId(R.id.loading_spinner), 10000);

            // Verify count is now 0 and empty state is shown
            onView(withId(R.id.empty_state_text)).check(matches(isDisplayed()));
            onView(withText("No organizers found.")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDeleteOrganizerCascadesEventDeletion() throws Exception {
        DatabaseHandler dbHandler = testHelper.getDbHandler();

        // 1. Setup Data: Create an organizer and an event organized by them.
        String orgId = "org_to_delete";
        Organizer targetOrg = new Organizer(orgId, "target@org.com", "Target", "Organizer", null);
        Tasks.await(targetOrg.save(), 5, TimeUnit.SECONDS);

        // Ensure registration window is open
        Date pastStart = new Date(System.currentTimeMillis() - 3600000);
        Date futureEnd = new Date(System.currentTimeMillis() + 3600000);

        Event event = Tasks.await(Event.create("Target Event", "Description", targetOrg, "University of Alberta", null, pastStart, futureEnd, new Date(), new Date(), false, 0.0, null, false), 5, TimeUnit.SECONDS);
        Tasks.await(event.save(), 5, TimeUnit.SECONDS);

        // Verify initial state in DB
        DocumentSnapshot userDoc = Tasks.await(dbHandler.getUser(orgId), 5, TimeUnit.SECONDS);
        assertEquals("Target", userDoc.getString("firstName"));

        QuerySnapshot eventSnap = Tasks.await(dbHandler.getDb().collection(DatabaseHandler.COLLECTION_EVENTS).whereEqualTo("organizerId", orgId).get(), 5, TimeUnit.SECONDS);
        assertEquals(1, eventSnap.size());

        // 2. UI Action: Delete the Organizer
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Organizers"), 2000);
            onView(withText("Manage Organizers")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            // Click delete on the target organizer
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Target Organizer")), clickChildViewWithId(R.id.btn_delete_profile)));

            waitForView(withText("Delete Profile?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            // Wait for cascaded deletion to complete
            waitForViewToDisappear(withId(R.id.loading_spinner), 10000);
        }

        // 3. Final Verification: Check DB to confirm the event is gone
        userDoc = Tasks.await(dbHandler.getUser(orgId), 5, TimeUnit.SECONDS);
        assertNull(userDoc.getData());

        eventSnap = Tasks.await(dbHandler.getDb().collection(DatabaseHandler.COLLECTION_EVENTS).whereEqualTo("organizerId", orgId).get(), 5, TimeUnit.SECONDS);
        assertEquals(0, eventSnap.size());
    }

    @Test
    public void testDeleteUserAccountWaitlistAndAcceptedCleanup() throws Exception {
        DatabaseHandler dbHandler = testHelper.getDbHandler();

        // 1. Setup: Create an event and two entrants.
        Organizer organizer = new Organizer("org_stays", "org@stays.com", "Bob", "Organizer", null);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);

        // Ensure registration window is open
        Date pastStart = new Date(System.currentTimeMillis() - 3600000);
        Date futureEnd = new Date(System.currentTimeMillis() + 3600000);

        Event event = Tasks.await(Event.create("Persistent Event", "Desc", organizer, "University of Alberta", null, pastStart, futureEnd, new Date(), new Date(), false, 0.0, null, false), 5, TimeUnit.SECONDS);
        Tasks.await(event.save(), 5, TimeUnit.SECONDS);

        // User A (on waitlist)
        String userIdA = "user_waiting";
        Entrant userA = new Entrant(userIdA, "waiting@test.com", "Waiting", "User", null);
        Tasks.await(userA.save(), 5, TimeUnit.SECONDS);
        Tasks.await(event.addEntrant(userA, null), 5, TimeUnit.SECONDS);

        // User B (accepted/selected)
        String userIdB = "user_accepted";
        Entrant userB = new Entrant(userIdB, "accepted@test.com", "Accepted", "User", null);
        Tasks.await(userB.save(), 5, TimeUnit.SECONDS);
        Tasks.await(event.addEntrant(userB, null), 5, TimeUnit.SECONDS);
        // Explicitly set status to 'selected' for User B
        java.util.Map<String, Object> selectedStatus = new java.util.HashMap<>();
        selectedStatus.put("status", "selected");
        Tasks.await(dbHandler.updateEntrantStatus(String.valueOf(event.getEventId()), userIdB, selectedStatus), 5, TimeUnit.SECONDS);

        // Verify initial DB state
        assertEquals(2, Tasks.await(dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId())), 5, TimeUnit.SECONDS).size());

        // 2. UI Action: Delete both users
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());
            waitForView(withText("Manage Profiles"), 2000);
            onView(withText("Manage Profiles")).perform(click());
            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            // Delete User A (Waiting)
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Waiting User")), clickChildViewWithId(R.id.btn_delete_profile)));
            waitForView(withText("Delete Profile?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());
            waitForViewToDisappear(withId(R.id.loading_spinner), 10000);

            // Delete User B (Accepted)
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Accepted User")), clickChildViewWithId(R.id.btn_delete_profile)));
            waitForView(withText("Delete Profile?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());
            waitForViewToDisappear(withId(R.id.loading_spinner), 10000);
        }

        // 3. Final Verification: Confirm registrations are scrubbed from the Event collection
        QuerySnapshot entrantsSnap = Tasks.await(dbHandler.getEntrantsForEvent(String.valueOf(event.getEventId())), 5, TimeUnit.SECONDS);
        assertEquals(0, entrantsSnap.size());

        // Also confirm profiles are gone
        assertNull(Tasks.await(dbHandler.getUser(userIdA), 5, TimeUnit.SECONDS).getData());
        assertNull(Tasks.await(dbHandler.getUser(userIdB), 5, TimeUnit.SECONDS).getData());
    }

    @Test
    public void testAdminCannotDeleteSelf() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withText("Manage Profiles"), 2000);
            onView(withText("Manage Profiles")).perform(click());

            waitForViewToDisappear(withId(R.id.loading_spinner), 5000);

            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(3));

            // Attempt to delete Admin User (the current user)
            onView(withId(R.id.profiles_recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("Admin User")), clickChildViewWithId(R.id.btn_delete_profile)));

            // The modal should NOT appear because app disables self-deletion
            onView(withText("Delete Profile?")).check(doesNotExist());

            // Item count should remain 3
            onView(withId(R.id.profiles_recycler_view)).check(hasItemCount(3));
        }
    }
}
