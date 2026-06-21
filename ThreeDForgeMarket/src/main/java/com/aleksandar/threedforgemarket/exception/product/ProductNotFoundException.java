package com.aleksandar.threedforgemarket.exception.product;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super("Product with id '" + productId + "' was not found.");
    }
}
