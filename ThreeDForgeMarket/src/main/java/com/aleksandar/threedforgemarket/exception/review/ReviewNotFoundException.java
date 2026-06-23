package com.aleksandar.threedforgemarket.exception.review;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException() {
        super("The requested review was not found.");
    }
}
