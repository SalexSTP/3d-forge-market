package com.aleksandar.threedforgemarket.service.payment;

public record StripeCheckoutResult(String sessionId, String checkoutUrl) {
}
