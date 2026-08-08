package com.aleksandar.threedforgemarket.service.product;

import com.aleksandar.threedforgemarket.config.CacheConfiguration;
import com.aleksandar.threedforgemarket.exception.product.ProductDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.product.ProductNameAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.mapper.product.ProductMapper;
import com.aleksandar.threedforgemarket.model.dto.product.ProductCatalogItemDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductFormDto;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private final CustomerOrderRepository customerOrderRepository;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, CustomerOrderRepository customerOrderRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Cacheable(
            cacheNames = CacheConfiguration.PRODUCT_CATALOG,
            key = "{T(org.springframework.util.StringUtils).hasText(#search) ? #search.trim() : null, #productCategory}"
    )
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

    @Cacheable(cacheNames = CacheConfiguration.PRODUCT_DETAILS, key = "#productId")
    public ProductDetailsDto getAvailableProductDetails(UUID productId) {
        Product product = productRepository.findByIdAndAvailableTrue(productId)
                .orElseThrow(ProductNotFoundException::new);

        return productMapper.toDetailsDto(product);
    }

    public ProductDetailsDto getProductDetailsForAdmin(UUID productId) {
        Product product = findProductById(productId);

        return productMapper.toDetailsDto(product);
    }

    @Cacheable(cacheNames = CacheConfiguration.FEATURED_PRODUCTS)
    public List<ProductCatalogItemDto> getFeaturedProducts() {
        return productRepository.findTop3ByAvailableTrueOrderByCreatedOnDesc()
                .stream()
                .map(productMapper::toCatalogItemDto)
                .toList();
    }

    @Cacheable(
            cacheNames = CacheConfiguration.ADMIN_PRODUCTS,
            key = "{T(org.springframework.util.StringUtils).hasText(#search) ? #search.trim() : null, #category, #material, #minPrice, #maxPrice, #available}"
    )
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
    @CacheEvict(
            cacheNames = {
                    CacheConfiguration.FEATURED_PRODUCTS,
                    CacheConfiguration.PRODUCT_CATALOG,
                    CacheConfiguration.PRODUCT_DETAILS,
                    CacheConfiguration.ADMIN_PRODUCTS
            },
            allEntries = true
    )
    public void createProduct(ProductFormDto productForm) {
        validateProductName(productForm.getName(), null);

        Product product = productMapper.toEntity(productForm);

        Product savedProduct = productRepository.save(product);
        LOGGER.info("Created product with id={}", savedProduct.getId());
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    CacheConfiguration.FEATURED_PRODUCTS,
                    CacheConfiguration.PRODUCT_CATALOG,
                    CacheConfiguration.PRODUCT_DETAILS,
                    CacheConfiguration.ADMIN_PRODUCTS
            },
            allEntries = true
    )
    public void updateProduct(UUID productId, ProductFormDto productForm) {
        Product product = findProductById(productId);

        validateProductName(productForm.getName(), productId);

        productMapper.updateEntity(product, productForm);

        productRepository.save(product);
        LOGGER.info("Updated product with id={}", product.getId());
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    CacheConfiguration.FEATURED_PRODUCTS,
                    CacheConfiguration.PRODUCT_CATALOG,
                    CacheConfiguration.PRODUCT_DETAILS,
                    CacheConfiguration.ADMIN_PRODUCTS
            },
            allEntries = true
    )
    public void toggleProductAvailability(UUID productId) {
        Product product = findProductById(productId);

        product.setAvailable(!product.isAvailable());

        productRepository.save(product);
        LOGGER.info("Changed product availability to {} for product id={}", product.isAvailable(), product.getId());
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    CacheConfiguration.FEATURED_PRODUCTS,
                    CacheConfiguration.PRODUCT_CATALOG,
                    CacheConfiguration.PRODUCT_DETAILS,
                    CacheConfiguration.ADMIN_PRODUCTS
            },
            allEntries = true
    )
    public void deleteProduct(UUID productId) {
        Product product = findProductById(productId);

        if (customerOrderRepository.existsByProduct_Id(productId)) {
            LOGGER.info("Blocked product deletion because order history exists for product id={}", productId);
            throw new ProductDeletionNotAllowedException();
        }

        productRepository.delete(product);
        LOGGER.info("Deleted product with id={}", productId);
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
