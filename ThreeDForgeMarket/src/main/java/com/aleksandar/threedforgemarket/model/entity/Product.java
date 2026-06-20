package com.aleksandar.threedforgemarket.model.entity;

import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String modelUrl;

    private Integer estimatedPrintTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory productCategory;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false)
    private LocalDateTime updatedOn;

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdOn == null) {
            createdOn = now;
        }

        updatedOn = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedOn = LocalDateTime.now();
    }
}