package com.lab.booking.model;

public enum BookingStatus {
    CONFIRMED("CONFIRMED"),
    CHECKED_IN("CHECKED_IN / BOARDED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BookingStatus getNextAdminStatus() {
        if (this == CONFIRMED) {
            return CHECKED_IN;
        } else if (this == CHECKED_IN) {
            return COMPLETED;
        }
        return null;
    }

    public boolean canCancel() {
        return this == CONFIRMED;
    }
}
