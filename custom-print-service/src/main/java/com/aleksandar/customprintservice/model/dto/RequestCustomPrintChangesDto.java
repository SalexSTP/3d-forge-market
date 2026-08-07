package com.aleksandar.customprintservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestCustomPrintChangesDto(
        @NotBlank(message = "Customer message is required.")
        @Size(min = 5, max = 1000, message = "Customer message must be between 5 and 1000 characters.")
        String customerMessage
) {
}
