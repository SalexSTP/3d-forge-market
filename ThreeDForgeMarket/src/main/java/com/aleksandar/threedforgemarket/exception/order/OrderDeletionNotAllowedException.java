package com.aleksandar.threedforgemarket.exception.order;

public class OrderDeletionNotAllowedException extends RuntimeException {
    public OrderDeletionNotAllowedException() {
        super("Only delivered or cancelled orders can be removed from history.");
    }
}
