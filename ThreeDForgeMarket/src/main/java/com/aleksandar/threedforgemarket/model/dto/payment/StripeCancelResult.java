package com.aleksandar.threedforgemarket.model.dto.payment;

import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;

import java.util.UUID;

public record StripeCancelResult(
        PaymentTargetType targetType,
        UUID productId,
        UUID customPrintRequestId,
        CreateOrderRequest orderForm
) {

    public static StripeCancelResult productOrder(UUID productId, CreateOrderRequest orderForm) {
        return new StripeCancelResult(PaymentTargetType.PRODUCT_ORDER, productId, null, orderForm);
    }

    public static StripeCancelResult customPrintRequest(UUID customPrintRequestId) {
        return new StripeCancelResult(PaymentTargetType.CUSTOM_PRINT_REQUEST, null, customPrintRequestId, null);
    }
}
