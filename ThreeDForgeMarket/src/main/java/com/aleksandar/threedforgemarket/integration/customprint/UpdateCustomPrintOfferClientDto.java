package com.aleksandar.threedforgemarket.integration.customprint;

import java.math.BigDecimal;

public record UpdateCustomPrintOfferClientDto(
        BigDecimal quotedPrice,
        Integer estimatedPrintTimeMinutes,
        String adminMessage,
        CustomPrintRequestStatus status
) {
}
