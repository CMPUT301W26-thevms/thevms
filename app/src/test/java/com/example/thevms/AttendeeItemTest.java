package com.example.thevms;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.Entrant;

/**
 * Unit tests for AttendeeItem.
 * Tests the isCancellable() logic — organizer can only cancel a "selected"
 * entrant who hasn't made a decision yet.
 */
public class AttendeeItemTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Entrant makeEntrant() {
        return new Entrant("device_001", "test@example.com", "John", "Doe", null);
    }

    private AttendeeItem makeItem(String status) {
        return new AttendeeItem(makeEntrant(), status);
    }

    // ── isCancellable() ───────────────────────────────────────────────────────

    @Test
    public void isCancellable_whenSelected_returnsTrue() {
        AttendeeItem item = makeItem("selected");
        assertTrue(item.isCancellable());
    }

    @Test
    public void isCancellable_whenWaiting_returnsFalse() {
        AttendeeItem item = makeItem("waiting");
        assertFalse(item.isCancellable());
    }

    @Test
    public void isCancellable_whenAccepted_returnsFalse() {
        AttendeeItem item = makeItem("accepted");
        assertFalse(item.isCancellable());
    }

    @Test
    public void isCancellable_whenRejected_returnsFalse() {
        AttendeeItem item = makeItem("rejected");
        assertFalse(item.isCancellable());
    }

    @Test
    public void isCancellable_whenCancelled_returnsFalse() {
        AttendeeItem item = makeItem("cancelled");
        assertFalse(item.isCancellable());
    }

    // ── Null status handling ──────────────────────────────────────────────────

    @Test
    public void nullStatus_defaultsToWaiting() {
        AttendeeItem item = new AttendeeItem(makeEntrant(), null);
        // Null status should default to "waiting", not throw
        assertEquals("waiting", item.getStatus());
        assertFalse(item.isCancellable());
    }

    // ── getStatus / setStatus ─────────────────────────────────────────────────

    @Test
    public void setStatus_updatesStatusCorrectly() {
        AttendeeItem item = makeItem("waiting");
        item.setStatus("accepted");
        assertEquals("accepted", item.getStatus());
        assertFalse(item.isCancellable());
    }

    @Test
    public void setStatus_toSelected_makesCancellable() {
        AttendeeItem item = makeItem("waiting");
        item.setStatus("selected");
        assertTrue(item.isCancellable());
    }

    // ── getEntrant ────────────────────────────────────────────────────────────

    @Test
    public void getEntrant_returnsCorrectEntrant() {
        Entrant entrant = makeEntrant();
        AttendeeItem item = new AttendeeItem(entrant, "waiting");
        assertEquals(entrant, item.getEntrant());
    }
}