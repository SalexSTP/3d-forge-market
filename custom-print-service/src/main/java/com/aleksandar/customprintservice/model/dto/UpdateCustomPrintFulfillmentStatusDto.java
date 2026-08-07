package com.aleksandar.customprintservice.model.dto;

import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomPrintFulfillmentStatusDto(
        @NotNull(message = "Fulfillment status is required.")
        CustomPrintRequestStatus status
) {
}
