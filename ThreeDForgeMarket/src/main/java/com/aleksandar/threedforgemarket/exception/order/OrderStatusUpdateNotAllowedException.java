package com.aleksandar.threedforgemarket.exception.order;

public class OrderStatusUpdateNotAllowedException extends RuntimeException {
    public OrderStatusUpdateNotAllowedException() {
        super("The selected order status update is not allowed.");
    }
}
