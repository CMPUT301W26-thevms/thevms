package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
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
    private Long testEventId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        Context context = ApplicationProvider.getApplicationContext();
        adminDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Seed an admin user
        Entrant admin = new Entrant(adminDeviceId, "admin@example.com", "Admin", "User", "1234567890", true, UserRole.ADMIN);
        Tasks.await(admin.save(), 5, TimeUnit.SECONDS);

        // Seed an event with a real valid image (simulated with a generated bitmap)
        Organizer organizer = new Organizer("org1", "org@test.com", "Bob", "Organizer", null);
        Tasks.await(organizer.save(), 5, TimeUnit.SECONDS);

        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] realPhotoBytes = stream.toByteArray();

        Event event = Tasks.await(Event.create("Image Event", "Desc", organizer, "Location", realPhotoBytes, new Date(), new Date(), new Date(), new Date(), false, 0.0, null, false), 5, TimeUnit.SECONDS);
        testEventId = event.getEventId();
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
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());

            waitForView(withId(R.id.admin_manage_images), 2000);
            onView(withId(R.id.admin_manage_images)).perform(click());

            waitForView(withId(R.id.admin_images_title_text), 2000);
            onView(withId(R.id.admin_images_title_text)).check(matches(withText("Manage Images")));

            onView(withText("Image Event")).check(matches(isDisplayed()));
            onView(withId(R.id.images_recycler_view)).check(hasItemCount(1));

            onView(withId(R.id.btn_delete_image)).perform(click());

            waitForView(withText("Delete Image?"), 2000);
            onView(withId(R.id.btn_dialog_cancel)).perform(click());

            onView(withText("Delete Image?")).check(doesNotExist());
            onView(withText("Image Event")).check(matches(isDisplayed()));
            onView(withId(R.id.images_recycler_view)).check(hasItemCount(1));
        }
    }

    @Test
    public void testDeleteImageSucceeds() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());
            waitForView(withId(R.id.admin_manage_images), 2000);
            onView(withId(R.id.admin_manage_images)).perform(click());

            waitForView(withText("Image Event"), 2000);
            onView(withId(R.id.images_recycler_view)).check(hasItemCount(1));

            onView(withId(R.id.btn_delete_image)).perform(click());
            waitForView(withText("Delete Image?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());

            waitForView(withId(R.id.empty_state_text), 5000);
            onView(withId(R.id.empty_state_text)).check(matches(isDisplayed()));
            onView(withId(R.id.images_recycler_view)).check(hasItemCount(0));
        }
    }

    @Test
    public void testDeleteImageRemovesFromEventAndDB() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 1. Navigate and Delete from Manage Images
            waitForView(withId(R.id.nav_admin_settings), 5000);
            onView(withId(R.id.nav_admin_settings)).perform(click());
            waitForView(withId(R.id.admin_manage_images), 2000);
            onView(withId(R.id.admin_manage_images)).perform(click());

            onView(withId(R.id.btn_delete_image)).perform(click());
            waitForView(withText("Delete Image?"), 2000);
            onView(withId(R.id.btn_dialog_delete)).perform(click());
            waitForView(withId(R.id.empty_state_text), 5000);

            // 2. Verify it is gone from DB
            DocumentSnapshot doc = Tasks.await(testHelper.getDbHandler().getDb()
                    .collection(DatabaseHandler.COLLECTION_EVENTS)
                    .document(String.valueOf(testEventId)).get(), 5, TimeUnit.SECONDS);
            assertNull("Photo should be null in DB", doc.get("photo"));

            // 3. Navigate to Manage Events and verify event is still there but image is gone
            // Note: Since we are already in a sub-fragment, we click the admin gear again to reset or use back.
            // But usually, clicking the gear again opens the drawer or we can just navigate to the other fragment via drawer.
            onView(withId(R.id.nav_admin_settings)).perform(click());
            waitForView(withId(R.id.admin_manage_event), 2000);
            onView(withId(R.id.admin_manage_event)).perform(click());

            waitForView(withText("Image Event"), 2000);
            // Even though the card is there, the image view should be empty (hard to test, but confirming the event persists after image deletion is valuable).
            onView(withText("Image Event")).check(matches(isDisplayed()));
        }
    }
}
