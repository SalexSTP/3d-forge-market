package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.config.StripeProperties;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class StripeCheckoutClientImpl implements StripeCheckoutClient {
    private final StripeProperties stripeProperties;

    public StripeCheckoutClientImpl(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @Override
    public StripeCheckoutResult createCheckoutSession(StripeCheckoutRequest request) throws StripeException {
        Stripe.apiKey = stripeProperties.secretKey();

        SessionCreateParams params = buildCreateParams(request);

        Session session = Session.create(params);

        return new StripeCheckoutResult(session.getId(), session.getUrl());
    }

    SessionCreateParams buildCreateParams(StripeCheckoutRequest request) {
        SessionCreateParams.LineItem.PriceData.ProductData.Builder productDataBuilder =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(request.productName());

        if (request.productImageUrl() != null) {
            productDataBuilder.addImage(request.productImageUrl());
        }

        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setLocale(SessionCreateParams.Locale.EN)
                .setBrandingSettings(SessionCreateParams.BrandingSettings.builder()
                        .setDisplayName(stripeProperties.checkoutDisplayName())
                        .build())
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .putMetadata("paymentTransactionId", request.paymentTransactionId().toString())
                .putMetadata("targetType", request.targetType().name())
                .putMetadata("targetId", request.targetId().toString())
                .putMetadata("customerId", request.customerId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(request.currency())
                                .setUnitAmount(toCents(request.amount()))
                                .setProductData(productDataBuilder.build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public StripeWebhookSession verifyCheckoutSessionEvent(
            String payload,
            String signatureHeader,
            String expectedEventType
    ) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.webhookSecret());

        if (!expectedEventType.equals(event.getType())) {
            return null;
        }

        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (!(stripeObject instanceof Session session)) {
            return null;
        }

        String paymentIntentId = session.getPaymentIntent();
        Map<String, String> metadata = session.getMetadata() == null ? Map.of() : session.getMetadata();

        return new StripeWebhookSession(session.getId(), paymentIntentId, metadata);
    }

    private long toCents(BigDecimal amount) {
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}
