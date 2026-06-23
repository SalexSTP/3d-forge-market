package com.aleksandar.threedforgemarket.model.dto.order;

import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AdminOrderListItemDto {
    private final UUID id;

    private final String customerUsername;
    private final String customerEmail;

    private final UUID productId;
    private final String productName;

    private final Integer quantity;
    private final BigDecimal totalPrice;

    private final String deliveryAddress;
    private final String customerNote;

    private final LocalDateTime createdOn;
    private final LocalDateTime updatedOn;
    private final OrderStatus status;

    private final List<OrderStatus> availableStatusUpdates;
    private final boolean deletable;
}
