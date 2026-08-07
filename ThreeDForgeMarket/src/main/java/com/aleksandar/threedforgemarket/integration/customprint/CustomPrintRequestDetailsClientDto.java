package com.aleksandar.threedforgemarket.integration.customprint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomPrintRequestDetailsClientDto(
        UUID id,
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
        LocalDateTime acceptedOn
) {

    public boolean isPendingReview() {
        return status == CustomPrintRequestStatus.PENDING_REVIEW;
    }

    public boolean isOfferSent() {
        return status == CustomPrintRequestStatus.OFFER_SENT;
    }

    public boolean isChangesRequested() {
        return status == CustomPrintRequestStatus.CHANGES_REQUESTED;
    }

    public boolean isCancellable() {
        return status == CustomPrintRequestStatus.PENDING_REVIEW
                || status == CustomPrintRequestStatus.OFFER_SENT
                || status == CustomPrintRequestStatus.CHANGES_REQUESTED;
    }

    public boolean isAcceptable() {
        return status == CustomPrintRequestStatus.OFFER_SENT;
    }

    public boolean isChangeRequestAllowed() {
        return status == CustomPrintRequestStatus.OFFER_SENT;
    }

    public boolean isCustomerRemovable() {
        return status == CustomPrintRequestStatus.CANCELLED
                || status == CustomPrintRequestStatus.REJECTED;
    }

    public boolean isAdminArchivable() {
        return status == CustomPrintRequestStatus.CANCELLED
                || status == CustomPrintRequestStatus.REJECTED;
    }

    public boolean isAdminActionable() {
        return isPendingReview() || isChangesRequested();
    }
}
