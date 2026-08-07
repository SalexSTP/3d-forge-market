package com.aleksandar.threedforgemarket.integration.customprint;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCustomPrintRequestClientDto(
        UUID customerId,
        String customerUsername,
        String customerEmail,
        String title,
        String description,
        String material,
        String colorDescription,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal depthCm,
        Integer quantity,
        String referenceFileUrl
) {
}
