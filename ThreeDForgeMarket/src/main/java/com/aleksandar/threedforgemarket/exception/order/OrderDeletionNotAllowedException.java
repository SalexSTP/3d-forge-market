package com.aleksandar.threedforgemarket.exception.order;

public class OrderDeletionNotAllowedException extends RuntimeException {
    public OrderDeletionNotAllowedException() {
        super("Only cancelled orders can be deleted from your history.");
    }
}
