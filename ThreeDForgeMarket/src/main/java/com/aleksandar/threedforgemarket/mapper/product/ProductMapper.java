package com.aleksandar.threedforgemarket.mapper.product;

import com.aleksandar.threedforgemarket.model.dto.product.ProductCatalogItemDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductFormDto;
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

    public ProductFormDto toFormDto(Product product) {
        ProductFormDto productForm = new ProductFormDto();

        productForm.setId(product.getId());
        productForm.setName(product.getName());
        productForm.setDescription(product.getDescription());
        productForm.setPrice(product.getPrice());
        productForm.setImageUrl(product.getImageUrl());
        productForm.setModelUrl(product.getModelUrl());
        productForm.setEstimatedPrintTimeMinutes(product.getEstimatedPrintTimeMinutes());
        productForm.setWidthCm(product.getWidthCm());
        productForm.setHeightCm(product.getHeightCm());
        productForm.setDepthCm(product.getDepthCm());
        productForm.setWeightGrams(product.getWeightGrams());
        productForm.setProductCategory(product.getProductCategory());
        productForm.setMaterial(product.getMaterial());
        productForm.setColorDescription(product.getColorDescription());
        productForm.setAvailable(product.isAvailable());

        return productForm;
    }

    public Product toEntity(ProductFormDto productForm) {
        return Product.builder()
                .name(productForm.getName().strip())
                .description(productForm.getDescription().strip())
                .price(productForm.getPrice())
                .imageUrl(productForm.getImageUrl().strip())
                .modelUrl(normalizeOptionalUrl(productForm.getModelUrl()))
                .estimatedPrintTimeMinutes(productForm.getEstimatedPrintTimeMinutes())
                .widthCm(productForm.getWidthCm())
                .heightCm(productForm.getHeightCm())
                .depthCm(productForm.getDepthCm())
                .weightGrams(productForm.getWeightGrams())
                .productCategory(productForm.getProductCategory())
                .material(productForm.getMaterial())
                .colorDescription(productForm.getColorDescription().strip())
                .available(productForm.isAvailable())
                .build();
    }

    public void updateEntity(Product product, ProductFormDto productForm) {
        product.setName(productForm.getName().strip());
        product.setDescription(productForm.getDescription().strip());
        product.setPrice(productForm.getPrice());
        product.setImageUrl(productForm.getImageUrl().strip());
        product.setModelUrl(normalizeOptionalUrl(productForm.getModelUrl()));
        product.setEstimatedPrintTimeMinutes(productForm.getEstimatedPrintTimeMinutes());
        product.setWidthCm(productForm.getWidthCm());
        product.setHeightCm(productForm.getHeightCm());
        product.setDepthCm(productForm.getDepthCm());
        product.setWeightGrams(productForm.getWeightGrams());
        product.setProductCategory(productForm.getProductCategory());
        product.setMaterial(productForm.getMaterial());
        product.setColorDescription(productForm.getColorDescription().strip());
        product.setAvailable(productForm.isAvailable());
    }

    private String normalizeOptionalUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        return url.strip();
    }
}
