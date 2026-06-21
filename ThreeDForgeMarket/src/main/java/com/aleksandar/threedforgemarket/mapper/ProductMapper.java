package com.aleksandar.threedforgemarket.mapper;

import com.aleksandar.threedforgemarket.model.dto.product.ProductCatalogItemDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductCatalogItemDto toCatalogItemDto(Product product) {
        return ProductCatalogItemDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .estimatedPrintTimeMinutes(product.getEstimatedPrintTimeMinutes())
                .widthCm(product.getWidthCm())
                .heightCm(product.getHeightCm())
                .depthCm(product.getDepthCm())
                .productCategory(product.getProductCategory())
                .material(product.getMaterial())
                .available(product.isAvailable())
                .build();
    }

    public ProductDetailsDto toDetailsDto(Product product) {
        return ProductDetailsDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .modelUrl(product.getModelUrl())
                .estimatedPrintTimeMinutes(product.getEstimatedPrintTimeMinutes())
                .widthCm(product.getWidthCm())
                .heightCm(product.getHeightCm())
                .depthCm(product.getDepthCm())
                .weightGrams(product.getWeightGrams())
                .productCategory(product.getProductCategory())
                .material(product.getMaterial())
                .colorDescription(product.getColorDescription())
                .available(product.isAvailable())
                .build();
    }
}
