package com.aleksandar.threedforgemarket.exception.payment;

public class StripePaymentUnavailableException extends RuntimeException {

    public StripePaymentUnavailableException() {
        super("Online payments are currently unavailable.");
    }
}
