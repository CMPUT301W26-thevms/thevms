package com.example.thevms.model;

/**
 * Wraps an Entrant with their event-specific status.
 * Since a user can be part of many events with different statuses,
 * status lives here rather than on the Entrant model itself.
 */
public class AttendeeItem {
    private final Entrant entrant;
    private String status;

    public AttendeeItem(Entrant entrant, String status) {
        this.entrant = entrant;
        this.status = status != null ? status : "waiting";
    }

    public Entrant getEntrant() {
        return entrant;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCancellable() {
        return "selected".equals(status);
    }
}