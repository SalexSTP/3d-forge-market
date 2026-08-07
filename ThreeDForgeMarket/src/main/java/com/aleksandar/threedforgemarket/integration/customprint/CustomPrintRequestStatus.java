package com.aleksandar.threedforgemarket.integration.customprint;

public enum CustomPrintRequestStatus {
    PENDING_REVIEW("Pending review"),
    OFFER_SENT("Offer sent"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String displayName;

    CustomPrintRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
