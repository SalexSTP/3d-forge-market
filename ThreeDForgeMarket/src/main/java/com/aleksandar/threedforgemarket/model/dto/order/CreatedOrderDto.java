package com.aleksandar.threedforgemarket.model.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatedOrderDto(
        UUID id,
        UUID customerId,
        UUID productId,
        String productName,
        String productImageUrl,
        BigDecimal totalPrice
) {
}
