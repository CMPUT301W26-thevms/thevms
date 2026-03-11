package com.example.thevms.ui.Event;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.Entrant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented tests for AttendeeAdapter filtering logic.
 * Runs on device/emulator because RecyclerView.Adapter requires Android runtime.
 *
 * Place in: src/androidTest/java/com/example/thevms/ui/Event/
 */
@RunWith(AndroidJUnit4.class)
public class AttendeeAdapterTest {

    private AttendeeAdapter adapter;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Entrant makeEntrant(String id, String firstName, String lastName) {
        return new Entrant(id, id + "@test.com", firstName, lastName, null);
    }

    private AttendeeItem makeItem(String id, String firstName, String lastName, String status) {
        return new AttendeeItem(makeEntrant(id, firstName, lastName), status);
    }

    private List<AttendeeItem> makeMixedList() {
        List<AttendeeItem> list = new ArrayList<>();
        list.add(makeItem("001", "Alice",   "Smith",   "waiting"));
        list.add(makeItem("002", "Bob",     "Jones",   "waiting"));
        list.add(makeItem("003", "Charlie", "Brown",   "selected"));
        list.add(makeItem("004", "Diana",   "Prince",  "accepted"));
        list.add(makeItem("005", "Eve",     "Adams",   "rejected"));
        list.add(makeItem("006", "Frank",   "Castle",  "cancelled"));
        return list;
    }

    @Before
    public void setUp() {
        adapter = new AttendeeAdapter();
    }

    // ── Default state ─────────────────────────────────────────────────────────

    @Test
    public void initialItemCount_isZero() {
        assertEquals(0, adapter.getItemCount());
    }

    // ── setAttendees + default filter ─────────────────────────────────────────

    @Test
    public void setAttendees_defaultFilter_showsOnlyWaiting() {
        adapter.setAttendees(makeMixedList());
        // Default activeStatus is "waiting" — should show 2 waiting entrants
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void setAttendees_emptyList_showsZero() {
        adapter.setAttendees(new ArrayList<>());
        assertEquals(0, adapter.getItemCount());
    }

    // ── filterByStatus ────────────────────────────────────────────────────────

    @Test
    public void filterByStatus_waiting_returnsCorrectCount() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("waiting");
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void filterByStatus_selected_returnsCorrectCount() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("selected");
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void filterByStatus_accepted_returnsCorrectCount() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("accepted");
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void filterByStatus_rejected_returnsCorrectCount() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("rejected");
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void filterByStatus_cancelled_returnsCorrectCount() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("cancelled");
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void filterByStatus_noMatches_returnsZero() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("accepted");
        // Only 1 accepted — then filter for something with no matches
        adapter.filterByStatus("nonexistent");
        assertEquals(0, adapter.getItemCount());
    }

    // ── Filter persists across setAttendees calls ─────────────────────────────

    @Test
    public void filterPersists_whenAttendeesReloaded() {
        adapter.setAttendees(makeMixedList());
        adapter.filterByStatus("selected");
        assertEquals(1, adapter.getItemCount());

        // Simulate a Firestore reload with a new list
        List<AttendeeItem> newList = new ArrayList<>(makeMixedList());
        newList.add(makeItem("007", "Grace", "Hopper", "selected")); // add another selected
        adapter.setAttendees(newList);

        // Filter should still be "selected" and now show 2
        assertEquals(2, adapter.getItemCount());
    }

    // ── All waiting entrants are isCancellable = false ────────────────────────

    @Test
    public void waitingEntrants_areNotCancellable() {
        List<AttendeeItem> list = new ArrayList<>();
        list.add(makeItem("001", "Alice", "Smith", "waiting"));
        list.add(makeItem("002", "Bob",   "Jones", "waiting"));
        adapter.setAttendees(list);
        adapter.filterByStatus("waiting");

        for (AttendeeItem item : list) {
            assertFalse(item.isCancellable());
        }
    }

    // ── Only selected entrants are cancellable ────────────────────────────────

    @Test
    public void selectedEntrants_areCancellable() {
        List<AttendeeItem> list = new ArrayList<>();
        list.add(makeItem("003", "Charlie", "Brown", "selected"));
        adapter.setAttendees(list);
        adapter.filterByStatus("selected");

        assertTrue(list.get(0).isCancellable());
    }

    @Test
    public void acceptedEntrants_areNotCancellable() {
        List<AttendeeItem> list = new ArrayList<>();
        list.add(makeItem("004", "Diana", "Prince", "accepted"));
        adapter.setAttendees(list);

        assertFalse(list.get(0).isCancellable());
    }

    @Test
    public void rejectedEntrants_areNotCancellable() {
        List<AttendeeItem> list = new ArrayList<>();
        list.add(makeItem("005", "Eve", "Adams", "rejected"));
        adapter.setAttendees(list);

        assertFalse(list.get(0).isCancellable());
    }
}