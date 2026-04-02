package com.example.thevms;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class MainPageMapTest {

    private FirestoreTestHelper testHelper;
    private String deviceId;

    @Before
    public void setUp() throws Exception {
        testHelper = new FirestoreTestHelper();
        testHelper.clearDatabase();

        deviceId = Settings.Secure.getString(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        Entrant user = new Entrant(deviceId, "map@test.com", "Map", "User", "123", true, UserRole.ENTRANT);
        Tasks.await(user.save(), 5, TimeUnit.SECONDS);

        Organizer organizer = new Organizer(deviceId, "org@test.com", "Org", "Owner", "123");

        Calendar cal = Calendar.getInstance();
        Date regStart = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date regEnd = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date eventStart = cal.getTime();
        cal.add(Calendar.HOUR, 2);
        Date eventEnd = cal.getTime();

        Event downtownA = Tasks.await(Event.create(
                "Downtown A", "Desc", organizer, "Loc A", null,
                regStart, regEnd, eventStart, eventEnd,
                false, 0.0, null, false
        ), 5, TimeUnit.SECONDS);
        downtownA.setGeoLocation(buildLocation(53.54610, -113.49380));
        Tasks.await(downtownA.save(), 5, TimeUnit.SECONDS);

        Event downtownB = Tasks.await(Event.create(
                "Downtown B", "Desc", organizer, "Loc B", null,
                regStart, regEnd, eventStart, eventEnd,
                false, 0.0, null, false
        ), 5, TimeUnit.SECONDS);
        downtownB.setGeoLocation(buildLocation(53.54610, -113.49380));
        Tasks.await(downtownB.save(), 5, TimeUnit.SECONDS);

        Event southSide = Tasks.await(Event.create(
                "South Side", "Desc", organizer, "Loc C", null,
                regStart, regEnd, eventStart, eventEnd,
                false, 0.0, null, false
        ), 5, TimeUnit.SECONDS);
        southSide.setGeoLocation(buildLocation(53.52000, -113.52000));
        Tasks.await(southSide.save(), 5, TimeUnit.SECONDS);
    }

    @Test
    public void selectingMapPinFiltersAndClearingRestoresList() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(2000);

            onView(withId(R.id.results_count_text)).check(matches(withText(containsString("3 results"))));

            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof SearchFragment) {
                    SearchFragment searchFragment = (SearchFragment) fragment;
                    searchFragment.selectMarkerForTesting(53.54610, -113.49380);
                    searchFragment.expandBottomSheet();
                }
            });

            Thread.sleep(1000);

            onView(withId(R.id.results_count_text)).check(matches(withText(containsString("2 results"))));
            onView(withId(R.id.events_recycler_view))
                    .check(matches(hasDescendant(withText("Downtown A"))));
            onView(withId(R.id.events_recycler_view))
                    .check(matches(hasDescendant(withText("Downtown B"))));

            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment instanceof SearchFragment) {
                    SearchFragment searchFragment = (SearchFragment) fragment;
                    searchFragment.clearSelectedMarkerForTesting();
                    searchFragment.expandBottomSheet();
                }
            });

            Thread.sleep(1000);

            onView(withId(R.id.results_count_text)).check(matches(withText(containsString("3 results"))));
            onView(withId(R.id.events_recycler_view))
                    .check(matches(hasDescendant(withText("South Side"))));
        }
    }

    private android.location.Location buildLocation(double latitude, double longitude) {
        android.location.Location location = new android.location.Location("test");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }
}
