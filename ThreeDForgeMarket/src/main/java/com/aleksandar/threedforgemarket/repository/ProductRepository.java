package com.aleksandar.threedforgemarket.repository;

import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByAvailableTrueOrderByCreatedOnDesc();

    List<Product> findAllByAvailableTrueAndProductCategoryOrderByCreatedOnDesc(
            ProductCategory productCategory
    );

    List<Product> findAllByAvailableTrueAndNameContainingIgnoreCaseOrderByCreatedOnDesc(
            String name
    );

    List<Product> findAllByAvailableTrueAndProductCategoryAndNameContainingIgnoreCaseOrderByCreatedOnDesc(
            ProductCategory productCategory,
            String name
    );

    List<Product> findTop3ByAvailableTrueOrderByCreatedOnDesc();

    Optional<Product> findByIdAndAvailableTrue(UUID id);

    List<Product> findAllByOrderByCreatedOnDesc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
