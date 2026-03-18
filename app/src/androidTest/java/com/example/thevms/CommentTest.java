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

import android.provider.Settings;
import android.view.View;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CommentTest {

    private FirestoreTestHelper testHelper;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();
        testHelper.seedDummyEvents(1);

        // Create a user profile so we have a name for comments
        String deviceId = Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "John");
        userData.put("lastName", "Doe");
        Tasks.await(testHelper.getDbHandler().getDb().collection("users").document(deviceId).set(userData), 5, TimeUnit.SECONDS);

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
        Thread.sleep(1000);

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
