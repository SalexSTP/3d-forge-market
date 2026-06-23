package com.aleksandar.threedforgemarket.exception.review;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException() {
        super("You have already submitted a review for this product.");
    }
}