package com.example.thevms.model;

/**
 * Wraps an Entrant with their event-specific status.
 * Since a user can be part of many events with different statuses,
 * status lives here rather than on the Entrant model itself.
 */
public class AttendeeItem {
    private final Entrant entrant;
    private String status;

    /**
     * Constructs a new AttendeeItem.
     *
     * @param entrant The entrant associated with this item.
     * @param status  The status of the entrant for a specific event.
     */
    public AttendeeItem(Entrant entrant, String status) {
        this.entrant = entrant;
        this.status = status != null ? status : "waiting";
    }

    /**
     * Gets the entrant.
     *
     * @return The entrant.
     */
    public Entrant getEntrant() {
        return entrant;
    }

    /**
     * Gets the status of the entrant.
     *
     * @return The status string.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the entrant.
     *
     * @param status The new status string.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if the entrant's status allows for cancellation.
     *
     * @return True if the status is "selected", false otherwise.
     */
    public boolean isCancellable() {
        return "selected".equals(status);
    }
}
