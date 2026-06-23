package com.aleksandar.threedforgemarket.config.seed;

import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class InitialProductDataConfiguration {
    @Bean
    public CommandLineRunner seedProducts(ProductRepository productRepository) {
        return arguments -> {
            if (productRepository.count() > 0) {
                return;
            }

            Product cableOrganizer = Product.builder()
                    .name("Desktop Cable Organizer")
                    .description("A compact cable organizer designed to keep charging cables neat and accessible on a desk.")
                    .price(new BigDecimal("6.90"))
                    .imageUrl("https://ustmfcfelfxhagvmawul.supabase.co/storage/v1/object/public/product-assets/images/desktop-cable-organizer.jpg")
                    .modelUrl("https://ustmfcfelfxhagvmawul.supabase.co/storage/v1/object/public/product-assets/models/desktop-cable-organizer.glb")
                    .estimatedPrintTimeMinutes(95)
                    .widthCm(new BigDecimal("8.50"))
                    .heightCm(new BigDecimal("2.50"))
                    .depthCm(new BigDecimal("5.00"))
                    .weightGrams(new BigDecimal("28.00"))
                    .productCategory(ProductCategory.ACCESSORY)
                    .material(PrintMaterial.PLA)
                    .colorDescription("Matte black")
                    .available(true)
                    .build();

            Product invisibleMountHook = Product.builder()
                    .name("Modern Simple Hook with Invisible Mount")
                    .description("A minimalist wall hook with a hidden mount profile, designed to stay visually clean while holding everyday items securely.")
                    .price(new BigDecimal("4.90"))
                    .imageUrl("https://ustmfcfelfxhagvmawul.supabase.co/storage/v1/object/public/product-assets/images/mount-hook.jpg")
                    .modelUrl("https://ustmfcfelfxhagvmawul.supabase.co/storage/v1/object/public/product-assets/models/mount-hook.glb")
                    .estimatedPrintTimeMinutes(71)
                    .widthCm(new BigDecimal("2.00"))
                    .heightCm(new BigDecimal("9.00"))
                    .depthCm(new BigDecimal("3.00"))
                    .weightGrams(new BigDecimal("15.00"))
                    .productCategory(ProductCategory.DECORATION)
                    .material(PrintMaterial.PLA)
                    .colorDescription("Matte black")
                    .available(true)
                    .build();

            productRepository.saveAll(List.of(
                    cableOrganizer,
                    invisibleMountHook
            ));
        };
    }
}
