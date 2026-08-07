package com.aleksandar.threedforgemarket.integration.customprint;

public enum CustomPrintRequestStatus {
    PENDING_REVIEW("Pending review"),
    OFFER_SENT("Offer sent"),
    CHANGES_REQUESTED("Changes requested"),
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
