package com.aleksandar.threedforgemarket.model.enums.payment;

public enum PaymentMethod {
    CASH_ON_DELIVERY("Cash on delivery"),
    STRIPE_CHECKOUT("Stripe Checkout");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
