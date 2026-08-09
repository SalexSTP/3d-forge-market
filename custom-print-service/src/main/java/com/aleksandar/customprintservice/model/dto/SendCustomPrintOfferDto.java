package com.aleksandar.customprintservice.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SendCustomPrintOfferDto(
        @NotNull(message = "Offer price is required.")
        @Positive(message = "Offer price must be positive.")
        BigDecimal quotedPrice,

        @NotNull(message = "Estimated print time is required.")
        @Min(value = 1, message = "Estimated print time must be at least 1 minute.")
        Integer estimatedPrintTimeMinutes,

        @Size(max = 1000, message = "Admin message must be at most 1000 characters.")
        String adminMessage,

        @Size(max = 500, message = "Response file URL must be at most 500 characters.")
        String responseFileUrl
) {
}
