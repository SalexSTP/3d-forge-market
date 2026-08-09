package com.aleksandar.threedforgemarket.model.dto.customprint;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomPrintOfferFormDto {
    @NotNull(message = "Offer price is required.")
    @Positive(message = "Offer price must be positive.")
    private BigDecimal quotedPrice;

    @NotNull(message = "Estimated print time is required.")
    @Min(value = 1, message = "Estimated print time must be at least 1 minute.")
    private Integer estimatedPrintTimeMinutes;

    @Size(max = 1000, message = "Admin message must not exceed 1000 characters.")
    private String adminMessage;

    @Size(max = 500, message = "Response file URL must not exceed 500 characters.")
    private String responseFileUrl;
}
