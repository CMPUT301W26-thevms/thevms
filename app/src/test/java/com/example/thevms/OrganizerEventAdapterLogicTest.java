package com.example.thevms;

import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.Entrant;
import com.example.thevms.ui.Event.OrganizerEventAdapter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Unit tests for the cancel + auto-select logic used in OrganizerEventAdapter.
 *
 * Since the actual Firestore calls can't be unit tested without a live DB,
 * we extract and test the pure logic:
 *   - Which entrant gets cancelled
 *   - How the next waiting entrant is selected
 *   - Edge cases (empty waitlist, single entrant, all accepted)
 */
public class OrganizerEventAdapterLogicTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Entrant makeEntrant(String id) {
        return new Entrant(id, id + "@test.com", "First_" + id, "Last_" + id, null);
    }

    private AttendeeItem makeItem(String id, String status) {
        return new AttendeeItem(makeEntrant(id), status);
    }

    /**
     * Mirrors the cancel + promote logic from OrganizerEventAdapter.cancelEntrantAndSelectNext()
     * as a pure function we can test without Firestore.
     *
     * Returns the updated list after cancelling the given entrantId
     * and randomly promoting one waiting entrant.
     */
    private List<AttendeeItem> simulateCancelAndSelectNext(
            List<AttendeeItem> items, String cancelledEntrantId, Random random) {

        // Step 1: cancel the target entrant
        for (AttendeeItem item : items) {
            if (item.getEntrant().getDeviceId().equals(cancelledEntrantId)) {
                item.setStatus("cancelled");
                break;
            }
        }

        // Step 2: collect all waiting entrants
        List<AttendeeItem> waiting = new ArrayList<>();
        for (AttendeeItem item : items) {
            if ("waiting".equals(item.getStatus())) {
                waiting.add(item);
            }
        }

        // Step 3: promote one at random if any exist
        if (!waiting.isEmpty()) {
            AttendeeItem next = waiting.get(random.nextInt(waiting.size()));
            next.setStatus("selected");
        }

        return items;
    }

    // ── Cancel logic ──────────────────────────────────────────────────────────

    @Test
    public void cancel_selectedEntrant_setsStatusToCancelled() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "selected"));
        items.add(makeItem("002", "waiting"));

        simulateCancelAndSelectNext(items, "001", new Random(42));

        assertEquals("cancelled", items.get(0).getStatus());
    }

    @Test
    public void cancel_doesNotAffectOtherEntrants_statuses() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "selected"));
        items.add(makeItem("002", "accepted"));
        items.add(makeItem("003", "rejected"));

        simulateCancelAndSelectNext(items, "001", new Random(42));

        assertEquals("accepted", items.get(1).getStatus());
        assertEquals("rejected", items.get(2).getStatus());
    }

    // ── Auto-select next waiting entrant ──────────────────────────────────────

    @Test
    public void afterCancel_oneWaitingEntrant_getsPromotedToSelected() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "selected"));
        items.add(makeItem("002", "waiting"));

        simulateCancelAndSelectNext(items, "001", new Random(42));

        assertEquals("selected", items.get(1).getStatus());
    }

    @Test
    public void afterCancel_multipleWaiting_exactlyOneGetsPromoted() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "selected"));
        items.add(makeItem("002", "waiting"));
        items.add(makeItem("003", "waiting"));
        items.add(makeItem("004", "waiting"));

        simulateCancelAndSelectNext(items, "001", new Random(42));

        // Count how many are now "selected"
        int selectedCount = 0;
        for (AttendeeItem item : items) {
            if ("selected".equals(item.getStatus())) selectedCount++;
        }
        assertEquals(1, selectedCount);
    }

    @Test
    public void afterCancel_emptyWaitlist_noOneGetsPromoted() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "selected"));
        items.add(makeItem("002", "accepted"));
        items.add(makeItem("003", "rejected"));

        simulateCancelAndSelectNext(items, "001", new Random(42));

        // No one should have been promoted — no waiting entrants exist
        int selectedCount = 0;
        for (AttendeeItem item : items) {
            if ("selected".equals(item.getStatus())) selectedCount++;
        }
        assertEquals(0, selectedCount);
    }

    @Test
    public void afterCancel_onlyAcceptedAndRejected_noOneGetsPromoted() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "selected"));
        items.add(makeItem("002", "accepted"));
        items.add(makeItem("003", "accepted"));

        simulateCancelAndSelectNext(items, "001", new Random(42));

        for (AttendeeItem item : items) {
            assertNotEquals("The cancelled entrant should not be re-selected",
                    "selected", items.get(0).getStatus());
        }
    }

    // ── isCancellable guard ───────────────────────────────────────────────────

    @Test
    public void onlySelectedEntrant_canBePassedToCancel() {
        List<AttendeeItem> items = new ArrayList<>();
        items.add(makeItem("001", "waiting"));
        items.add(makeItem("002", "selected"));
        items.add(makeItem("003", "accepted"));
        items.add(makeItem("004", "rejected"));
        items.add(makeItem("005", "cancelled"));

        // Only "selected" should pass the isCancellable() check
        int cancellableCount = 0;
        for (AttendeeItem item : items) {
            if (item.isCancellable()) cancellableCount++;
        }
        assertEquals(1, cancellableCount);
        assertTrue(items.get(1).isCancellable()); // only item 002 (selected)
    }

    // ── Random selection is within bounds ────────────────────────────────────

    @Test
    public void randomSelection_alwaysPicksFromWaitingList() {
        // Run 50 iterations with different seeds to ensure we never pick
        // a non-waiting entrant
        for (int seed = 0; seed < 50; seed++) {
            List<AttendeeItem> items = new ArrayList<>();
            items.add(makeItem("001", "selected"));
            items.add(makeItem("002", "waiting"));
            items.add(makeItem("003", "waiting"));
            items.add(makeItem("004", "accepted"));

            simulateCancelAndSelectNext(items, "001", new Random(seed));

            // accepted entrant should never have been changed
            assertEquals("accepted", items.get(3).getStatus());

            // cancelled entrant should not be re-promoted
            assertEquals("cancelled", items.get(0).getStatus());
        }
    }

    // ── Winner count helper ───────────────────────────────────────────────────

    @Test
    public void determineWinnerCount_limitsToRequestedAndWaiting() {
        int winners = OrganizerEventAdapter.ViewHolder.determineWinnerCount(3, 10, null, 0);
        assertEquals(3, winners);

        winners = OrganizerEventAdapter.ViewHolder.determineWinnerCount(8, 5, null, 0);
        assertEquals(5, winners);
    }

    @Test
    public void determineWinnerCount_respectsCapacity() {
        int winners = OrganizerEventAdapter.ViewHolder.determineWinnerCount(5, 10, 12, 9);
        // Only 3 spots remain
        assertEquals(3, winners);
    }

    @Test
    public void determineWinnerCount_returnsZeroWhenFullOrInvalid() {
        int winners = OrganizerEventAdapter.ViewHolder.determineWinnerCount(5, 0, 10, 2);
        assertEquals(0, winners);

        winners = OrganizerEventAdapter.ViewHolder.determineWinnerCount(5, 10, 10, 10);
        assertEquals(0, winners);
    }
}
