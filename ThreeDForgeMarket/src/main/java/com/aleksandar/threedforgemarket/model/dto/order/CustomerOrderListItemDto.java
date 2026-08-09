package com.aleksandar.threedforgemarket.model.dto.order;

import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentSummaryDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CustomerOrderListItemDto {
    private final UUID id;

    private final UUID productId;
    private final String productName;

    private final Integer quantity;
    private final BigDecimal totalPrice;
    private final OrderStatus status;
    private final PaymentSummaryDto paymentSummary;

    private final LocalDateTime createdOn;
    private final LocalDateTime updatedOn;

    private final String deliveryAddress;
    private final String customerNote;

    private final boolean cancellable;
    private final boolean deletable;
}
