package com.aleksandar.threedforgemarket.exception.order;

public class OrderCancellationNotAllowedException extends RuntimeException {
    public OrderCancellationNotAllowedException() {
        super("This order can no longer be cancelled.");
    }
}
