package com.example.thevms.ui.Event;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.Entrant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented tests for AttendeeAdapter CSV export.
 * These run on a device/emulator because they need a real Context
 * for file creation and Intent launching.
 *
 * Place this file in: src/androidTest/java/com/example/thevms/ui/Event/
 */
@RunWith(AndroidJUnit4.class)
public class AttendeeAdapterInstrumentedTest {

    private AttendeeAdapter adapter;
    private Context context;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Entrant makeEntrant(String id, String firstName, String lastName, String email) {
        return new Entrant(id, email, firstName, lastName, null);
    }

    private AttendeeItem makeItem(String id, String firstName, String lastName, String email, String status) {
        return new AttendeeItem(makeEntrant(id, firstName, lastName, email), status);
    }

    private List<AttendeeItem> makeWaitingList() {
        List<AttendeeItem> list = new ArrayList<>();
        list.add(makeItem("001", "Alice",   "Smith",  "alice@test.com",   "waiting"));
        list.add(makeItem("002", "Bob",     "Jones",  "bob@test.com",     "waiting"));
        list.add(makeItem("003", "Charlie", "Brown",  "charlie@test.com", "selected"));
        return list;
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        adapter = new AttendeeAdapter();
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();

        // Clean up any CSV files created during tests
        File cacheDir = context.getCacheDir();
        File[] csvFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".csv"));
        if (csvFiles != null) {
            for (File f : csvFiles) f.delete();
        }
    }

    // ── CSV export — empty list ───────────────────────────────────────────────

    @Test
    public void exportCsv_emptyFilteredList_doesNotLaunchIntent() {
        adapter.setAttendees(new ArrayList<>());
        adapter.filterByStatus("waiting");

        // Toast requires the main thread — run the export call there
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() ->
                        adapter.exportFilteredListAsCsv(context, "TestEvent")
                );

        // No ACTION_CHOOSER intent should have been fired
        assertEquals(0, Intents.getIntents().size());
    }

    // ── CSV export — correct intent fired ────────────────────────────────────

    @Test
    public void exportCsv_withFilteredList_launchesShareIntent() {
        adapter.setAttendees(makeWaitingList());
        adapter.filterByStatus("waiting"); // 2 waiting entrants

        adapter.exportFilteredListAsCsv(context, "SummerFest");

        // createChooser() wraps our ACTION_SEND inside an ACTION_CHOOSER intent.
        // We match the outer ACTION_CHOOSER and verify the inner intent is ACTION_SEND + text/csv.
        Intents.intended(
                allOf(
                        IntentMatchers.hasAction(Intent.ACTION_CHOOSER),
                        IntentMatchers.hasExtra(
                                equalTo(Intent.EXTRA_INTENT),
                                allOf(
                                        IntentMatchers.hasAction(Intent.ACTION_SEND),
                                        IntentMatchers.hasType("text/csv")
                                )
                        )
                )
        );
    }

    // ── CSV export — file created in cache ────────────────────────────────────

    @Test
    public void exportCsv_createsFileInCache() {
        adapter.setAttendees(makeWaitingList());
        adapter.filterByStatus("waiting");
        adapter.exportFilteredListAsCsv(context, "SummerFest");

        File expectedFile = new File(context.getCacheDir(), "SummerFest_waiting.csv");
        assertTrue("CSV file should exist in cache dir", expectedFile.exists());
        assertTrue("CSV file should not be empty", expectedFile.length() > 0);
    }

    // ── CSV export — file name sanitized ─────────────────────────────────────

    @Test
    public void exportCsv_eventNameWithSpaces_createsSanitizedFile() {
        adapter.setAttendees(makeWaitingList());
        adapter.filterByStatus("waiting");
        adapter.exportFilteredListAsCsv(context, "My Cool Event!");

        // Spaces and special chars replaced with underscores
        File expectedFile = new File(context.getCacheDir(), "My_Cool_Event__waiting.csv");
        assertTrue("Sanitized CSV file should exist", expectedFile.exists());
    }

    // ── CSV export — only filtered entrants exported ──────────────────────────

    @Test
    public void exportCsv_onlyExportsFilteredStatus() {
        adapter.setAttendees(makeWaitingList());
        adapter.filterByStatus("waiting"); // only 2 waiting, not Charlie (selected)

        adapter.exportFilteredListAsCsv(context, "TestEvent");

        File csvFile = new File(context.getCacheDir(), "TestEvent_waiting.csv");
        assertTrue(csvFile.exists());

        // Read file and verify contents
        try {
            java.util.Scanner scanner = new java.util.Scanner(csvFile);
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            scanner.close();

            String csv = content.toString();
            // Header row should be present
            assertTrue(csv.contains("First Name,Last Name,Email,Status"));
            // Waiting entrants should be present
            assertTrue(csv.contains("Alice"));
            assertTrue(csv.contains("Bob"));
            // Selected entrant should NOT be in this export
            assertFalse(csv.contains("Charlie"));
        } catch (Exception e) {
            fail("Failed to read CSV file: " + e.getMessage());
        }
    }

    // ── CSV export — switches with filter ────────────────────────────────────

    @Test
    public void exportCsv_afterFilterChange_exportsNewFilter() {
        adapter.setAttendees(makeWaitingList());

        // First export waiting
        adapter.filterByStatus("waiting");
        adapter.exportFilteredListAsCsv(context, "TestEvent");
        File waitingFile = new File(context.getCacheDir(), "TestEvent_waiting.csv");
        assertTrue(waitingFile.exists());

        // Switch to selected and export
        adapter.filterByStatus("selected");
        adapter.exportFilteredListAsCsv(context, "TestEvent");
        File selectedFile = new File(context.getCacheDir(), "TestEvent_selected.csv");
        assertTrue(selectedFile.exists());

        // Read selected file — should only contain Charlie
        try {
            java.util.Scanner scanner = new java.util.Scanner(selectedFile);
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            String csv = content.toString();
            assertTrue(csv.contains("Charlie"));
            assertFalse(csv.contains("Alice"));
            assertFalse(csv.contains("Bob"));
        } catch (Exception e) {
            fail("Failed to read CSV file: " + e.getMessage());
        }
    }
}