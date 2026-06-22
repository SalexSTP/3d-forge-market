package com.aleksandar.threedforgemarket.exception.order;

public class CustomerOrderNotFoundException extends RuntimeException {
    public CustomerOrderNotFoundException() {
        super("The requested order was not found.");
    }
}
