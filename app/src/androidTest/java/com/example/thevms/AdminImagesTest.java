package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for Admin Image management.
 * Requires Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
public class AdminImagesTest {

    private FirestoreTestHelper testHelper;
    private String adminDeviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = ApplicationProvider.getApplicationContext();
        adminDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed an admin user
        Entrant admin = new Entrant(adminDeviceId, "admin@example.com", "Admin", "User", "1234567890", true, UserRole.ADMIN);
        Tasks.await(admin.save(), 5, TimeUnit.SECONDS);

        // Seed an event with a "photo" (simulated with a small byte array)
        Organizer organizer = new Organizer("org1", "org@test.com", "Bob", "Organizer", null);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);
        
        byte[] fakePhoto = new byte[]{1, 2, 3, 4};
        Event event = Tasks.await(Event.create("Image Event", "Desc", organizer, "Location", fakePhoto, new Date(), new Date(), new Date(), new Date(), false, 0.0, null, false), 5, TimeUnit.SECONDS);
        Tasks.await(event.save(), 5, TimeUnit.SECONDS);
    }

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

    public static ViewAssertion hasItemCount(int expectedCount) {
        return (view, noViewFoundException) -> {
            if (!(view instanceof RecyclerView)) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            if (adapter == null || adapter.getItemCount() != expectedCount) {
                throw new AssertionError("Item count mismatch. Expected: " + expectedCount + ", Actual: " + (adapter == null ? 0 : adapter.getItemCount()));
            }
        };
    }

    @Test
    public void testAdminCanNavigateToImagesAndCancelDelete() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Admin Panel (Gear icon)
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            // Click Manage Images (using the ID from layout_admin_panel.xml)
            waitForView(withId(R.id.admin_manage_images), 2000);
            onView(withId(R.id.admin_manage_images)).perform(click());

            // Verify title
            waitForView(withId(R.id.admin_images_title_text), 2000);
            onView(withId(R.id.admin_images_title_text)).check(matches(withText("Manage Images")));
            
            // Verify our seeded image is there by checking the source name (Event name)
            onView(withText("Image Event")).check(matches(isDisplayed()));
            onView(withId(R.id.images_recycler_view)).check(hasItemCount(1));

            // Click Delete to open modal (The button inside the card)
            onView(withId(R.id.btn_delete_image)).perform(click());

            // Verify styled confirmation modal components
            waitForView(withText("Delete Image?"), 2000);
            onView(withId(R.id.btn_dialog_cancel)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_dialog_delete)).check(matches(isDisplayed()));

            // Cancel the delete
            onView(withId(R.id.btn_dialog_cancel)).perform(click());

            // Verify modal is gone and image still exists
            onView(withText("Delete Image?")).check(doesNotExist());
            onView(withText("Image Event")).check(matches(isDisplayed()));
            onView(withId(R.id.images_recycler_view)).check(hasItemCount(1));
        }
    }
}
