package com.aleksandar.threedforgemarket.exception.payment;

public class PaymentOperationFailedException extends RuntimeException {

    public PaymentOperationFailedException(String message) {
        super(message);
    }
}
