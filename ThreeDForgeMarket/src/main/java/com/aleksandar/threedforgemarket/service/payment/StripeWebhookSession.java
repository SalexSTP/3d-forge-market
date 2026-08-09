package com.aleksandar.threedforgemarket.service.payment;

import java.util.Map;

public record StripeWebhookSession(
        String id,
        String paymentIntentId,
        String invoiceId,
        Map<String, String> metadata
) {
}
