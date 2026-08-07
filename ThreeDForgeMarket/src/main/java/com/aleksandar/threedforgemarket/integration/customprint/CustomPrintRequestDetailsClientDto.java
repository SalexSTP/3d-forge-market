package com.aleksandar.threedforgemarket.integration.customprint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
        LocalDateTime acceptedOn,
        LocalDateTime printingStartedOn,
        LocalDateTime readyForDeliveryOn,
        LocalDateTime deliveredOn
) {

    public boolean isPendingReview() {
        return status == CustomPrintRequestStatus.PENDING_REVIEW;
    }

    public boolean isEditable() {
        return isPendingReview();
    }

    public boolean isOfferSent() {
        return status == CustomPrintRequestStatus.OFFER_SENT;
    }

    public boolean isChangesRequested() {
        return status == CustomPrintRequestStatus.CHANGES_REQUESTED;
    }

    public boolean isAccepted() {
        return status == CustomPrintRequestStatus.ACCEPTED;
    }

    public boolean isPrinting() {
        return status == CustomPrintRequestStatus.PRINTING;
    }

    public boolean isReadyForDelivery() {
        return status == CustomPrintRequestStatus.READY_FOR_DELIVERY;
    }

    public boolean isDelivered() {
        return status == CustomPrintRequestStatus.DELIVERED;
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
                || status == CustomPrintRequestStatus.REJECTED
                || status == CustomPrintRequestStatus.DELIVERED;
    }

    public boolean isAdminArchivable() {
        return status == CustomPrintRequestStatus.CANCELLED
                || status == CustomPrintRequestStatus.REJECTED
                || status == CustomPrintRequestStatus.DELIVERED;
    }

    public boolean isAdminActionable() {
        return isPendingReview() || isChangesRequested();
    }

    public boolean isFulfillmentVisible() {
        return isAccepted() || isPrinting() || isReadyForDelivery() || isDelivered();
    }

    public boolean isFulfillmentUpdatable() {
        return isAccepted() || isPrinting() || isReadyForDelivery();
    }

    public List<CustomPrintRequestStatus> getAvailableFulfillmentStatuses() {
        if (isAccepted()) {
            return List.of(CustomPrintRequestStatus.PRINTING);
        }

        if (isPrinting()) {
            return List.of(CustomPrintRequestStatus.READY_FOR_DELIVERY);
        }

        if (isReadyForDelivery()) {
            return List.of(CustomPrintRequestStatus.DELIVERED);
        }

        return List.of();
    }

    public CustomPrintRequestStatus getNextFulfillmentStatus() {
        List<CustomPrintRequestStatus> statuses = getAvailableFulfillmentStatuses();
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    public String getNextFulfillmentActionLabel() {
        CustomPrintRequestStatus nextStatus = getNextFulfillmentStatus();

        if (nextStatus == CustomPrintRequestStatus.PRINTING) {
            return "Start printing";
        }

        if (nextStatus == CustomPrintRequestStatus.READY_FOR_DELIVERY) {
            return "Mark ready for delivery";
        }

        if (nextStatus == CustomPrintRequestStatus.DELIVERED) {
            return "Mark delivered";
        }

        return "Update status";
    }
}
