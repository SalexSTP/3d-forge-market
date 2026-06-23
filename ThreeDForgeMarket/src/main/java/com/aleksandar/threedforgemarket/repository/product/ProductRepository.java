package com.aleksandar.threedforgemarket.repository.product;

import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Query("""
        SELECT p
        FROM Product p
        WHERE (:search IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:category IS NULL
                OR p.productCategory = :category)
          AND (:material IS NULL
                OR p.material = :material)
          AND (:minPrice IS NULL
                OR p.price >= :minPrice)
          AND (:maxPrice IS NULL
                OR p.price <= :maxPrice)
          AND (:available IS NULL
                OR p.available = :available)
        ORDER BY
            CASE
                WHEN p.available = true THEN 1
                ELSE 2
            END,
            p.createdOn DESC
        """)
    List<Product> findAllForAdmin(
            @Param("search") String search,
            @Param("category") ProductCategory category,
            @Param("material") PrintMaterial material,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("available") Boolean available
    );
}
