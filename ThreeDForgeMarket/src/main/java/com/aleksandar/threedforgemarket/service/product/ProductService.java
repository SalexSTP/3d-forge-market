package com.aleksandar.threedforgemarket.service.product;

import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.mapper.ProductMapper;
import com.aleksandar.threedforgemarket.model.dto.product.ProductCatalogItemDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductCatalogItemDto> getAvailableProducts(
            String search,
            ProductCategory productCategory
    ) {
        boolean hasSearch = StringUtils.hasText(search);
        String normalizedSearch = hasSearch ? search.trim() : null;

        List<Product> products;

        if (hasSearch && productCategory != null) {
            products = productRepository
                    .findAllByAvailableTrueAndProductCategoryAndNameContainingIgnoreCaseOrderByCreatedOnDesc(
                            productCategory,
                            normalizedSearch
                    );

        } else if (hasSearch) {
            products = productRepository
                    .findAllByAvailableTrueAndNameContainingIgnoreCaseOrderByCreatedOnDesc(
                            normalizedSearch
                    );

        } else if (productCategory != null) {
            products = productRepository
                    .findAllByAvailableTrueAndProductCategoryOrderByCreatedOnDesc(
                            productCategory
                    );

        } else {
            products = productRepository
                    .findAllByAvailableTrueOrderByCreatedOnDesc();
        }

        return products.stream()
                .map(productMapper::toCatalogItemDto)
                .toList();
    }

    public ProductDetailsDto getAvailableProductDetails(UUID productId) {
        Product product = productRepository.findByIdAndAvailableTrue(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return productMapper.toDetailsDto(product);
    }

    public List<ProductCatalogItemDto> getFeaturedProducts() {
        return productRepository.findTop3ByAvailableTrueOrderByCreatedOnDesc()
                .stream()
                .map(productMapper::toCatalogItemDto)
                .toList();
    }
}
