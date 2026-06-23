package com.aleksandar.threedforgemarket.exception.review;

public class ReviewEligibilityNotMetException extends RuntimeException {
    public ReviewEligibilityNotMetException() {
        super("You can submit a review only after this product has been delivered.");
    }
}
