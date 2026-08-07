package com.aleksandar.threedforgemarket.model.dto.customprint;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomPrintRequestFormDto {
    @NotBlank(message = "Title is required.")
    @Size(min = 5, max = 80, message = "Title must be between 5 and 80 characters.")
    private String title;

    @NotBlank(message = "Description is required.")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters.")
    private String description;

    @NotBlank(message = "Material is required.")
    @Size(min = 2, max = 50, message = "Material must be between 2 and 50 characters.")
    private String material;

    @NotBlank(message = "Color description is required.")
    @Size(min = 2, max = 80, message = "Color description must be between 2 and 80 characters.")
    private String colorDescription;

    @NotNull(message = "Width is required.")
    @Positive(message = "Width must be positive.")
    @DecimalMax(value = "500", message = "Width must be at most 500 cm.")
    private BigDecimal widthCm;

    @NotNull(message = "Height is required.")
    @Positive(message = "Height must be positive.")
    @DecimalMax(value = "500", message = "Height must be at most 500 cm.")
    private BigDecimal heightCm;

    @NotNull(message = "Depth is required.")
    @Positive(message = "Depth must be positive.")
    @DecimalMax(value = "500", message = "Depth must be at most 500 cm.")
    private BigDecimal depthCm;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 50, message = "Quantity cannot exceed 50.")
    private Integer quantity;

    @Size(max = 500, message = "Reference file URL must not exceed 500 characters.")
    private String referenceFileUrl;
}
