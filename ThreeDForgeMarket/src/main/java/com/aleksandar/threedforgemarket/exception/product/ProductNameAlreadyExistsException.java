package com.aleksandar.threedforgemarket.exception.product;

public class ProductNameAlreadyExistsException extends RuntimeException {
    public ProductNameAlreadyExistsException(String productName) {
        super("A product named '" + productName + "' already exists.");
    }
}
