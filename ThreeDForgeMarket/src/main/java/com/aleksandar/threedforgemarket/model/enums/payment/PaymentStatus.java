package com.aleksandar.threedforgemarket.model.enums.payment;

public enum PaymentStatus {
    PENDING("Pending"),
    PENDING_CASH_ON_DELIVERY("Cash on delivery pending"),
    PAID("Paid"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
