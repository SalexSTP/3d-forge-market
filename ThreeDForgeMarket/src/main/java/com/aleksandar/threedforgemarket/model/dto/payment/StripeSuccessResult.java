package com.aleksandar.threedforgemarket.model.dto.payment;

import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;

import java.util.UUID;

public record StripeSuccessResult(
        PaymentTargetType targetType,
        UUID targetId
) {
}
