package com.aleksandar.customprintservice.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCustomPrintRequestDto(
        @NotNull(message = "Customer ID is required.")
        UUID customerId,

        @NotBlank(message = "Customer username is required.")
        @Size(min = 3, max = 30, message = "Customer username must be between 3 and 30 characters.")
        String customerUsername,

        @NotBlank(message = "Customer email is required.")
        @Email(message = "Customer email must be valid.")
        @Size(max = 100, message = "Customer email must be at most 100 characters.")
        String customerEmail,

        @NotBlank(message = "Title is required.")
        @Size(min = 5, max = 80, message = "Title must be between 5 and 80 characters.")
        String title,

        @NotBlank(message = "Description is required.")
        @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters.")
        String description,

        @NotBlank(message = "Material is required.")
        @Size(min = 2, max = 50, message = "Material must be between 2 and 50 characters.")
        String material,

        @NotBlank(message = "Color description is required.")
        @Size(min = 2, max = 80, message = "Color description must be between 2 and 80 characters.")
        String colorDescription,

        @NotBlank(message = "Delivery address is required.")
        @Size(min = 10, max = 250, message = "Delivery address must be between 10 and 250 characters.")
        String deliveryAddress,

        @NotNull(message = "Width is required.")
        @Positive(message = "Width must be positive.")
        @DecimalMax(value = "500", message = "Width must be at most 500 cm.")
        BigDecimal widthCm,

        @NotNull(message = "Height is required.")
        @Positive(message = "Height must be positive.")
        @DecimalMax(value = "500", message = "Height must be at most 500 cm.")
        BigDecimal heightCm,

        @NotNull(message = "Depth is required.")
        @Positive(message = "Depth must be positive.")
        @DecimalMax(value = "500", message = "Depth must be at most 500 cm.")
        BigDecimal depthCm,

        @NotNull(message = "Quantity is required.")
        @Min(value = 1, message = "Quantity must be at least 1.")
        @Max(value = 50, message = "Quantity must be at most 50.")
        Integer quantity,

        @Size(max = 500, message = "Reference file URL must be at most 500 characters.")
        String referenceFileUrl
) {
}
