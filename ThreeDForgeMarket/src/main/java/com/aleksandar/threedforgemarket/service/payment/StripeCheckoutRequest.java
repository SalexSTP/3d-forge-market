package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;

import java.math.BigDecimal;
import java.util.UUID;

public record StripeCheckoutRequest(
        UUID paymentTransactionId,
        UUID customerId,
        PaymentTargetType targetType,
        UUID targetId,
        BigDecimal amount,
        String currency,
        String productName,
        String productImageUrl,
        String successUrl,
        String cancelUrl
) {
}
