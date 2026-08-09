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
        String deliveryAddress,
        CustomPrintRequestStatus status,
        BigDecimal quotedPrice,
        Integer estimatedPrintTimeMinutes,
        String adminMessage,
        String responseFileUrl,
        String customerMessage,
        LocalDateTime createdOn,
        LocalDateTime updatedOn,
        LocalDateTime quotedOn,
        LocalDateTime cancelledOn,
        LocalDateTime customerRespondedOn,
        LocalDateTime acceptedOn,
        LocalDateTime printingStartedOn,
        LocalDateTime readyForDeliveryOn,
        LocalDateTime deliveredOn,
        Boolean adminAttentionRequired,
        LocalDateTime adminAttentionMarkedOn,
        Boolean customerResponseReminderRequired,
        LocalDateTime customerResponseReminderMarkedOn
) {
}
