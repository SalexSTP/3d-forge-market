package com.aleksandar.threedforgemarket.integration.customprint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomPrintRequestListItemClientDto(
        UUID id,
        UUID customerId,
        String customerUsername,
        String customerEmail,
        String title,
        String material,
        String colorDescription,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal depthCm,
        Integer quantity,
        String referenceFileUrl,
        CustomPrintRequestStatus status,
        BigDecimal quotedPrice,
        Integer estimatedPrintTimeMinutes,
        String adminMessage,
        LocalDateTime createdOn,
        LocalDateTime updatedOn,
        LocalDateTime quotedOn,
        LocalDateTime cancelledOn
) {
}
