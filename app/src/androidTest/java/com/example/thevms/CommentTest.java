package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

import android.provider.Settings;
import android.view.View;
import android.widget.EditText;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.MyEventsActivity;
import com.example.thevms.ui.SearchFragment;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * UI tests for comment functionality across different user roles.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CommentTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        // Create a user profile so we have a name for comments
        String deviceId = Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "John");
        userData.put("lastName", "Doe");
        // Default to Admin role for tests that need to access admin panel
        userData.put("role", UserRole.ADMIN.name());
        Tasks.await(testHelper.getDbHandler().getDb().collection("users").document(deviceId).set(userData), 5, TimeUnit.SECONDS);

        // Seed an event so MainActivity has something to show
        testHelper.seedDummyEvents(1);

        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void testPostAndViewComment() throws InterruptedException {
        // 1. Expand bottom sheet to see the event card
        scenario.onActivity(activity -> {
            SearchFragment fragment = (SearchFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (fragment != null) {
                fragment.expandBottomSheet();
            }
        });
        Thread.sleep(2000); // Allow time for Firestore and UI expansion

        // 2. Click "View Comments" button
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, MainActivityTest.clickChildViewWithId(R.id.btn_view_comments)));
        Thread.sleep(1000);

        // 3. Verify comments section is displayed
        onView(withId(R.id.comments_section)).check(matches(isDisplayed()));

        // 4. Scroll the NestedScrollView to the bottom to ensure the comment bar is visible
        onView(withId(R.id.bottom_sheet)).perform(scrollToBottom());
        Thread.sleep(500);

        // 5. Type and post a comment
        String commentText = "This is a test comment " + System.currentTimeMillis();
        onView(withId(R.id.edit_comment)).perform(typeText(commentText), closeSoftKeyboard());
        onView(withId(R.id.btn_post_comment)).perform(click());
        Thread.sleep(2000);

        // 6. Verify comment is visible in the list
        onView(withId(R.id.comments_recycler_view))
                .check(matches(hasDescendant(withText(commentText))));

        // 7. Verify user name is visible
        onView(withId(R.id.comments_recycler_view))
                .check(matches(hasDescendant(withText("John"))));
        onView(withId(R.id.comments_recycler_view))
                .check(matches(hasDescendant(withText("Doe"))));
    }

    /**
     * Test to verify that when an organizer posts a comment from their dashboard,
     * the "ORGANIZER" tag is visible on the comment.
     */
    @Test
    public void testOrganizerPostAndViewComment() throws InterruptedException, ExecutionException, TimeoutException {
        testHelper.clearDatabase();
        String deviceId = Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // 1. Seed exactly one event for the current organizer
        testHelper.seedDummyEventsForOrganizer(1, deviceId);

        // 2. Launch MyEventsActivity (the Organizer's events page)
        try (ActivityScenario<MyEventsActivity> organizerScenario = ActivityScenario.launch(MyEventsActivity.class)) {
            Thread.sleep(2500);

            String organizerMessage = "Official update from the organizer";

            // 3. Target the EditText inside the first RecyclerView item directly Gemini was used here, no other solution worked :\
            onView(withId(R.id.rv_my_events))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, new ViewAction() {
                        @Override
                        public Matcher<View> getConstraints() {
                            return isDisplayed();
                        }

                        @Override
                        public String getDescription() {
                            return "Set organizer comment text";
                        }

                        @Override
                        public void perform(UiController uiController, View view) {
                            EditText et = view.findViewById(R.id.et_organizer_comment);
                            if (et != null) et.setText(organizerMessage);
                        }
                    }));

            // 4. Click the post button specifically within that same recycler view item
            onView(withId(R.id.rv_my_events))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                            MainActivityTest.clickChildViewWithId(R.id.btn_post_organizer_comment)));

            // Wait for Firestore round-trip
            Thread.sleep(2000);

            // 5. Verify the comment text is present
            onView(withId(R.id.rv_comments))
                    .check(matches(hasDescendant(withText(organizerMessage))));

            // 6. Verify the "ORGANIZER" tag is displayed for this comment
            onView(withId(R.id.rv_comments))
                    .check(matches(hasDescendant(allOf(
                            withId(R.id.tv_organizer_tag),
                            withText("ORGANIZER"),
                            isDisplayed()
                    ))));
        }
    }

    /**
     * Test for deleting a comment as an organizer.
     * Seeds one event with one comment, then deletes it and verifies it's gone.
     */
    @Test
    public void testDeleteCommentAsOrganizer() throws Exception {
        testHelper.clearDatabase();
        String deviceId = Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // 1. Seed exactly one event for the current organizer
        testHelper.seedDummyEventsForOrganizer(1, deviceId);

        // 2. Seed exactly one comment for that event
        // The first event seeded after clearDatabase will have ID "1"
        String commentContent = "Organizer test comment to delete";
        testHelper.seedComment("1", "otherUser", "Jane", "Smith", commentContent);

        // 3. Launch MyEventsActivity (the Organizer's events page)
        try (ActivityScenario<MyEventsActivity> organizerScenario = ActivityScenario.launch(MyEventsActivity.class)) {
            // Wait for Firestore data to load and RecyclerView to populate
            Thread.sleep(2500);

            // 4. Verify the comment is visible initially
            onView(withId(R.id.rv_comments))
                    .check(matches(hasDescendant(withText(commentContent))));

            // 5. Click the delete button for the first comment in the list
            onView(withId(R.id.rv_comments))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                            MainActivityTest.clickChildViewWithId(R.id.btn_delete_comment)));

            Thread.sleep(1500);

            // 6. Confirm the deletion in the confirmation dialog
            onView(withId(R.id.btn_dialog_yes)).perform(click());

            // Wait for the deletion to propagate to Firestore and the UI to refresh
            Thread.sleep(1500);

            // 7. Verify the comment is no longer displayed in the list
            onView(withId(R.id.rv_comments))
                    .check(matches(not(hasDescendant(withText(commentContent)))));
        }
    }

    /**
     * Test for deleting a comment as an administrator.
     * Seeds one event with exactly one comment, navigates to the admin events panel,
     * then deletes the comment and verifies it's gone.
     */
    @Test
    public void testDeleteCommentAsAdmin() throws Exception {
        // 1. Seed exactly one event and one comment
        testHelper.clearDatabase();
        testHelper.seedDummyEvents(1);
        String commentContent = "Admin test comment to delete";
        testHelper.seedComment("1", "otherUser", "Jane", "Smith", commentContent);

        // Ensure user is admin (setUp does this, but clearing DB above removed it)
        String deviceId = Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "Admin");
        userData.put("lastName", "User");
        userData.put("role", UserRole.ADMIN.name());
        Tasks.await(testHelper.getDbHandler().getDb().collection("users").document(deviceId).set(userData), 5, TimeUnit.SECONDS);

        // 2. Click the admin gear in bottom nav (nav_admin_settings)
        onView(withId(R.id.nav_admin_settings)).perform(click());
        Thread.sleep(1000);

        // 3. Click "Manage Event" in the admin drawer
        onView(withId(R.id.admin_manage_event)).perform(click());
        Thread.sleep(2000);

        // 4. Click "View Comments" on the first event card in the admin view
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, MainActivityTest.clickChildViewWithId(R.id.btn_view_comments)));
        Thread.sleep(1000);

        // 5. Verify the comment is visible
        onView(withId(R.id.comments_recycler_view))
                .check(matches(hasDescendant(withText(commentContent))));

        // 6. Click the delete button on the comment
        onView(withId(R.id.comments_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, MainActivityTest.clickChildViewWithId(R.id.btn_delete_comment)));

        Thread.sleep(1500);

        // 7. Confirm deletion in the dialog
        onView(withId(R.id.btn_dialog_yes)).perform(click());
        Thread.sleep(1500);

        // 8. Verify the comment is gone
        onView(withId(R.id.comments_recycler_view))
                .check(matches(not(hasDescendant(withText(commentContent)))));
    }

    /**
     * Custom ViewAction to scroll to the bottom of a NestedScrollView.
     */
    public static ViewAction scrollToBottom() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(NestedScrollView.class);
            }

            @Override
            public String getDescription() {
                return "scroll to bottom of NestedScrollView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                NestedScrollView nestedScrollView = (NestedScrollView) view;
                nestedScrollView.fullScroll(View.FOCUS_DOWN);
            }
        };
    }
}
