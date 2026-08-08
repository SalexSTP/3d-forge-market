package com.aleksandar.threedforgemarket.testdata;

import com.aleksandar.threedforgemarket.model.dto.product.ProductFormDto;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;

import java.math.BigDecimal;

public final class ProductTestData {

    private ProductTestData() {
    }

    public static Product product(String name) {
        return Product.builder()
                .name(name)
                .description("A reliable printed product for service tests.")
                .price(new BigDecimal("12.50"))
                .imageUrl("https://example.com/product.jpg")
                .modelUrl("https://example.com/product.glb")
                .estimatedPrintTimeMinutes(90)
                .widthCm(new BigDecimal("10.00"))
                .heightCm(new BigDecimal("5.00"))
                .depthCm(new BigDecimal("3.00"))
                .weightGrams(new BigDecimal("42.00"))
                .productCategory(ProductCategory.ACCESSORY)
                .material(PrintMaterial.PLA)
                .colorDescription("Black")
                .available(true)
                .build();
    }

    public static Product hiddenProduct(String name) {
        Product product = product(name);
        product.setAvailable(false);
        return product;
    }

    public static ProductFormDto productForm(String name) {
        ProductFormDto form = new ProductFormDto();
        form.setName(name);
        form.setDescription("A valid product form description.");
        form.setPrice(new BigDecimal("24.90"));
        form.setImageUrl("https://example.com/form-product.jpg");
        form.setModelUrl("https://example.com/form-product.glb");
        form.setEstimatedPrintTimeMinutes(120);
        form.setWidthCm(new BigDecimal("11.00"));
        form.setHeightCm(new BigDecimal("6.00"));
        form.setDepthCm(new BigDecimal("4.00"));
        form.setWeightGrams(new BigDecimal("50.00"));
        form.setProductCategory(ProductCategory.FIGURE);
        form.setMaterial(PrintMaterial.PETG);
        form.setColorDescription("Blue");
        form.setAvailable(true);
        return form;
    }
}
