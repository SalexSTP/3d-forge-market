package com.aleksandar.threedforgemarket.exception.order;

public class ProductDeletionNotAllowedException extends RuntimeException {
    public ProductDeletionNotAllowedException() {
        super("Products with order history cannot be deleted. Hide the product instead.");
    }
}
