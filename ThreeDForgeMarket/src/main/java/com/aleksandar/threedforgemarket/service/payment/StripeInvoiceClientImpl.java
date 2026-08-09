package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.config.StripeProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StripeInvoiceClientImpl implements StripeInvoiceClient {
    private final StripeProperties stripeProperties;

    public StripeInvoiceClientImpl(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @Override
    public Optional<String> getInvoicePdfUrl(String stripeInvoiceId) throws StripeException {
        if (stripeInvoiceId == null || stripeInvoiceId.isBlank()) {
            return Optional.empty();
        }

        Stripe.apiKey = stripeProperties.secretKey();
        Invoice invoice = Invoice.retrieve(stripeInvoiceId);

        return Optional.ofNullable(invoice.getInvoicePdf())
                .filter(value -> !value.isBlank());
    }
}
