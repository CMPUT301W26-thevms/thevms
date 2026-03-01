package com.example.thevms.model;

/**
 * Represents an entrant in an event.
 */
public class Entrant {
    private String email;
    private String username;
    private String phoneNumber;

    public Entrant(String email, String username, String phoneNumber) {
        this.email = email;
        this.username = username;
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void registerInEvent(Event event) {
        throw new UnsupportedOperationException("Not implemented yet");
//        event.addEntrant(this, Boolean.FALSE);
    }

    public void unregisterFromEvent(Event event) {
        throw new UnsupportedOperationException("Not implemented yet");
//        event.removeEntrant(this);
    }
}
