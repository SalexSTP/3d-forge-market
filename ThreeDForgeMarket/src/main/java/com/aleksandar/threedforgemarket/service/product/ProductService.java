package com.aleksandar.threedforgemarket.service.product;

import com.aleksandar.threedforgemarket.exception.product.ProductNameAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.mapper.product.ProductMapper;
import com.aleksandar.threedforgemarket.model.dto.product.ProductCatalogItemDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductFormDto;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
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
                .orElseThrow(ProductNotFoundException::new);

        return productMapper.toDetailsDto(product);
    }

    public List<ProductCatalogItemDto> getFeaturedProducts() {
        return productRepository.findTop3ByAvailableTrueOrderByCreatedOnDesc()
                .stream()
                .map(productMapper::toCatalogItemDto)
                .toList();
    }

    public List<ProductCatalogItemDto> getAllProductsForAdmin(
            String search,
            ProductCategory category,
            PrintMaterial material,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available
    ) {
        String normalizedSearch = StringUtils.hasText(search)
                ? search.trim()
                : null;

        return productRepository.findAllForAdmin(
                        normalizedSearch,
                        category,
                        material,
                        minPrice,
                        maxPrice,
                        available
                )
                .stream()
                .map(productMapper::toCatalogItemDto)
                .toList();
    }

    public ProductFormDto getProductForm(UUID productId) {
        Product product = findProductById(productId);

        return productMapper.toFormDto(product);
    }

    @Transactional
    public void createProduct(ProductFormDto productForm) {
        validateProductName(productForm.getName(), null);

        Product product = productMapper.toEntity(productForm);

        productRepository.save(product);
    }

    @Transactional
    public void updateProduct(UUID productId, ProductFormDto productForm) {
        Product product = findProductById(productId);

        validateProductName(productForm.getName(), productId);

        productMapper.updateEntity(product, productForm);

        productRepository.save(product);
    }

    @Transactional
    public void toggleProductAvailability(UUID productId) {
        Product product = findProductById(productId);

        product.setAvailable(!product.isAvailable());

        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = findProductById(productId);

        productRepository.delete(product);
    }

    private Product findProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private void validateProductName(String productName, UUID currentProductId) {
        String normalizedProductName = productName.strip();

        boolean nameAlreadyExists = currentProductId == null
                ? productRepository.existsByNameIgnoreCase(normalizedProductName)
                : productRepository.existsByNameIgnoreCaseAndIdNot(
                normalizedProductName,
                currentProductId
        );

        if (nameAlreadyExists) {
            throw new ProductNameAlreadyExistsException(normalizedProductName);
        }
    }
}
