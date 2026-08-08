package com.aleksandar.customprintservice.model.dto;

import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomPrintRequestListItemDto(
        UUID id,
        UUID customerId,
        String customerUsername,
        String customerEmail,
        String title,
        String material,
        String colorDescription,
        String deliveryAddress,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal depthCm,
        Integer quantity,
        String referenceFileUrl,
        String responseFileUrl,
        CustomPrintRequestStatus status,
        BigDecimal quotedPrice,
        Integer estimatedPrintTimeMinutes,
        String adminMessage,
        String customerMessage,
        LocalDateTime createdOn,
        LocalDateTime updatedOn,
        LocalDateTime quotedOn,
        LocalDateTime customerRespondedOn,
        LocalDateTime acceptedOn,
        LocalDateTime printingStartedOn,
        LocalDateTime readyForDeliveryOn,
        LocalDateTime deliveredOn,
        LocalDateTime cancelledOn,
        boolean adminAttentionRequired,
        LocalDateTime adminAttentionMarkedOn,
        boolean customerResponseReminderRequired,
        LocalDateTime customerResponseReminderMarkedOn
) {
}
