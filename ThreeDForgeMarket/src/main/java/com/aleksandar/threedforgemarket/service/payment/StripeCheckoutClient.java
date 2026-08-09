package com.aleksandar.threedforgemarket.service.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

public interface StripeCheckoutClient {

    StripeCheckoutResult createCheckoutSession(StripeCheckoutRequest request) throws StripeException;

    StripeWebhookSession verifyCheckoutSessionEvent(
            String payload,
            String signatureHeader,
            String expectedEventType
    ) throws SignatureVerificationException;
}
