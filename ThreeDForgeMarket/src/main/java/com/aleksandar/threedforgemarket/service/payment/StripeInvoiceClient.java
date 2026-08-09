package com.aleksandar.threedforgemarket.service.payment;

import com.stripe.exception.StripeException;

import java.util.Optional;

public interface StripeInvoiceClient {

    Optional<String> getInvoicePdfUrl(String stripeInvoiceId) throws StripeException;
}
