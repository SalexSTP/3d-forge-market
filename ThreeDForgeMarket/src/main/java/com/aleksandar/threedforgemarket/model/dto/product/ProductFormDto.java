package com.aleksandar.threedforgemarket.model.dto.product;

import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductFormDto {
    private UUID id;

    @NotBlank(message = "Product name is required.")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters.")
    private String name;

    @NotBlank(message = "Description is required.")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters.")
    private String description;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero.")
    private BigDecimal price;

    @NotBlank(message = "Image URL is required.")
    @Size(max = 500, message = "Image URL must not exceed 500 characters.")
    @Pattern(
            regexp = "^https://.+",
            message = "Image URL must be a valid HTTPS URL."
    )
    private String imageUrl;

    @Size(max = 500, message = "3D preview URL must not exceed 500 characters.")
    @Pattern(
            regexp = "^$|^https://.+",
            message = "3D preview URL must be a valid HTTPS URL."
    )
    private String modelUrl;

    @NotNull(message = "Estimated print time is required.")
    @Positive(message = "Estimated print time must be greater than zero.")
    private Integer estimatedPrintTimeMinutes;

    @NotNull(message = "Width is required.")
    @DecimalMin(value = "0.01", message = "Width must be greater than zero.")
    private BigDecimal widthCm;

    @NotNull(message = "Height is required.")
    @DecimalMin(value = "0.01", message = "Height must be greater than zero.")
    private BigDecimal heightCm;

    @NotNull(message = "Depth is required.")
    @DecimalMin(value = "0.01", message = "Depth must be greater than zero.")
    private BigDecimal depthCm;

    @NotNull(message = "Weight is required.")
    @DecimalMin(value = "0.01", message = "Weight must be greater than zero.")
    private BigDecimal weightGrams;

    @NotNull(message = "Product category is required.")
    private ProductCategory productCategory;

    @NotNull(message = "Print material is required.")
    private PrintMaterial material;

    @NotBlank(message = "Colour description is required.")
    @Size(max = 255, message = "Colour description must not exceed 255 characters.")
    private String colorDescription;

    private boolean available;
}
