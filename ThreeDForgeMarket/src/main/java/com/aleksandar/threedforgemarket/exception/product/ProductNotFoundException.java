package com.aleksandar.threedforgemarket.exception.product;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException() {
        super("The requested product was not found.");
    }
}
