package com.aleksandar.threedforgemarket.exception.order;

public class OrderCreationNotAllowedException extends RuntimeException {
    public OrderCreationNotAllowedException() {
        super("Only customer accounts can place orders.");
    }
}
