package com.aleksandar.threedforgemarket.model.dto.product;

import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ProductDetailsDto {
    private final UUID id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final String imageUrl;
    private final String modelUrl;
    private final Integer estimatedPrintTimeMinutes;
    private final BigDecimal widthCm;
    private final BigDecimal heightCm;
    private final BigDecimal depthCm;
    private final BigDecimal weightGrams;
    private final ProductCategory productCategory;
    private final PrintMaterial material;
    private final String colorDescription;
    private final boolean available;
}
