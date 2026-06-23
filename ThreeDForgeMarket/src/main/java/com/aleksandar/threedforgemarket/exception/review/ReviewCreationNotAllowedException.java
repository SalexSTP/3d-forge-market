package com.aleksandar.threedforgemarket.exception.review;

public class ReviewCreationNotAllowedException extends RuntimeException {
    public ReviewCreationNotAllowedException() {
        super("Only customer accounts can submit reviews.");
    }
}
