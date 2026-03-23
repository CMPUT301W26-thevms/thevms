package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNull;

import android.provider.Settings;
import android.view.View;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SignupActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * UI test for deleting own profile from ProfileFragment.
 * Ensures the app redirects to the signup page after deletion and the profile is removed from the database.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileDeleteTest {

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

        // Seed a test user so we can bypass the initial signup screen and reach the ProfileFragment
        Entrant user = new Entrant(deviceId, "test@example.com", "Test", "User", "1234567890");
        Tasks.await(user.save(), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testDeleteOwnProfileRedirectsToSignupAndDeletesFromDb() throws Exception {
        // Launch MainActivity directly since we seeded a user
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            
            // Wait for activity and role check to settle
            Thread.sleep(2000);

            // 1. Navigate to Profile tab
            onView(withId(R.id.nav_profile)).perform(click());

            // 2. Wait for ProfileFragment to load and data to be fetched
            onView(withId(R.id.profile_title)).check(matches(isDisplayed()));
            Thread.sleep(1500);

            // 3. Scroll to the bottom of the NestedScrollView to ensure the delete button is fully visible
            onView(allOf(isAssignableFrom(NestedScrollView.class), isDisplayed()))
                    .perform(scrollToBottom());
            
            // Brief pause for scroll animation/layout to settle
            Thread.sleep(1000);

            // 4. Click Delete Profile button
            onView(withId(R.id.btn_delete_profile)).perform(click());

            // 5. Verify confirmation dialog is displayed
            onView(withText("Delete Profile")).check(matches(isDisplayed()));
            
            // 6. Confirm deletion
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            // 7. Verify redirection to SignupActivity
            intended(hasComponent(SignupActivity.class.getName()));

            // 8. Verify that the profile was deleted in the database
            DatabaseHandler dbHandler = new DatabaseHandler();
            DocumentSnapshot doc = Tasks.await(dbHandler.getUser(deviceId), 5, TimeUnit.SECONDS);
            assertNull("Profile should be deleted from the database", Entrant.fromMap(deviceId, doc.getData()));
        }
    }

    /**
     * Custom ViewAction to scroll to the bottom of a NestedScrollView.
     */
    public static ViewAction scrollToBottom() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isAssignableFrom(NestedScrollView.class), isDisplayed());
            }

            @Override
            public String getDescription() {
                return "scroll to bottom of NestedScrollView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                NestedScrollView nestedScrollView = (NestedScrollView) view;
                nestedScrollView.fullScroll(View.FOCUS_DOWN);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
