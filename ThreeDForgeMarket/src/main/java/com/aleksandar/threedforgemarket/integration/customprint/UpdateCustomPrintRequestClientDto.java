package com.aleksandar.threedforgemarket.integration.customprint;

import java.math.BigDecimal;

public record UpdateCustomPrintRequestClientDto(
        String title,
        String description,
        String material,
        String colorDescription,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal depthCm,
        Integer quantity,
        String deliveryAddress,
        String referenceFileUrl
) {
}
