package com.aleksandar.threedforgemarket.exception.order;

public class ProductUnavailableException extends RuntimeException{
    public ProductUnavailableException() {
        super("The selected product is currently unavailable.");
    }
}
