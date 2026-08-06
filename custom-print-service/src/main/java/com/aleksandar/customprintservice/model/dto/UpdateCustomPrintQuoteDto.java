package com.aleksandar.customprintservice.model.dto;

import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateCustomPrintQuoteDto(
        @Positive(message = "Offer price must be positive.")
        BigDecimal quotedPrice,

        @Min(value = 1, message = "Estimated print time must be at least 1 minute.")
        Integer estimatedPrintTimeMinutes,

        @Size(max = 1000, message = "Admin message must be at most 1000 characters.")
        String adminMessage,

        @NotNull(message = "Status is required.")
        CustomPrintRequestStatus status
) {
}
