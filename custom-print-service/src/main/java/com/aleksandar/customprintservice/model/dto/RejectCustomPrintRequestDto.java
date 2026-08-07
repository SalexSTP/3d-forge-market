package com.aleksandar.customprintservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCustomPrintRequestDto(
        @NotBlank(message = "Admin message is required.")
        @Size(min = 5, max = 1000, message = "Admin message must be between 5 and 1000 characters.")
        String adminMessage
) {
}
