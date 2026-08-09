package com.aleksandar.threedforgemarket.service.product;

import com.aleksandar.threedforgemarket.exception.product.ProductDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.product.ProductNameAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.model.dto.product.ProductCatalogItemDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.dto.product.ProductFormDto;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.OrderTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void getFeaturedProductsReturnsLatestThreeAvailableProducts() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(1);
        productRepository.save(withCreatedOn(ProductTestData.product("First Stand"), baseTime));
        productRepository.save(withCreatedOn(ProductTestData.product("Second Stand"), baseTime.plusMinutes(1)));
        productRepository.save(withCreatedOn(ProductTestData.hiddenProduct("Hidden Stand"), baseTime.plusMinutes(5)));
        Product third = productRepository.save(withCreatedOn(ProductTestData.product("Third Stand"), baseTime.plusMinutes(2)));
        Product fourth = productRepository.save(withCreatedOn(ProductTestData.product("Fourth Stand"), baseTime.plusMinutes(3)));
        Product fifth = productRepository.save(withCreatedOn(ProductTestData.product("Fifth Stand"), baseTime.plusMinutes(4)));

        List<ProductCatalogItemDto> result = productService.getFeaturedProducts();

        assertThat(result)
                .extracting(ProductCatalogItemDto::getId)
                .containsExactly(fifth.getId(), fourth.getId(), third.getId());
    }

    @Test
    void getAvailableProductsAppliesSearchAndCategoryFilters() {
        productRepository.save(ProductTestData.product("Desk Cable Holder"));
        Product matching = ProductTestData.product("Desk Mini Figure");
        matching.setProductCategory(ProductCategory.FIGURE);
        matching = productRepository.save(matching);

        List<ProductCatalogItemDto> result = productService.getAvailableProducts(" mini ", ProductCategory.FIGURE);

        assertThat(result)
                .singleElement()
                .extracting(ProductCatalogItemDto::getId)
                .isEqualTo(matching.getId());
    }

    @Test
    void getAvailableProductDetailsReturnsAvailableProduct() {
        Product product = productRepository.save(ProductTestData.product("Visible Product"));

        ProductDetailsDto details = productService.getAvailableProductDetails(product.getId());

        assertThat(details.getId()).isEqualTo(product.getId());
        assertThat(details.getName()).isEqualTo("Visible Product");
    }

    @Test
    void getAvailableProductDetailsThrowsForHiddenOrMissingProduct() {
        Product hidden = productRepository.save(ProductTestData.hiddenProduct("Hidden Product"));

        assertThatThrownBy(() -> productService.getAvailableProductDetails(hidden.getId()))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> productService.getAvailableProductDetails(UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createProductPersistsFormData() {
        ProductFormDto form = ProductTestData.productForm("Created Product");

        productService.createProduct(form);

        assertThat(productRepository.findAll())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getName()).isEqualTo("Created Product");
                    assertThat(product.isAvailable()).isTrue();
                });
    }

    @Test
    void updateProductChangesExistingProduct() {
        Product product = productRepository.save(ProductTestData.product("Original Product"));
        ProductFormDto form = ProductTestData.productForm("Updated Product");

        productService.updateProduct(product.getId(), form);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.getProductCategory()).isEqualTo(ProductCategory.FIGURE);
    }

    @Test
    void toggleProductAvailabilityFlipsCurrentValue() {
        Product product = productRepository.save(ProductTestData.product("Toggle Product"));

        productService.toggleProductAvailability(product.getId());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isAvailable()).isFalse();
    }

    @Test
    void deleteProductSucceedsWhenNoOrderHistoryExists() {
        Product product = productRepository.save(ProductTestData.product("Delete Product"));

        productService.deleteProduct(product.getId());

        assertThat(productRepository.existsById(product.getId())).isFalse();
    }

    @Test
    void deleteProductIsBlockedWhenOrderHistoryExists() {
        User customer = userRepository.save(UserTestData.customer("customer"));
        Product product = productRepository.save(ProductTestData.product("Ordered Product"));
        customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.DELIVERED));

        assertThatThrownBy(() -> productService.deleteProduct(product.getId()))
                .isInstanceOf(ProductDeletionNotAllowedException.class);
        assertThat(productRepository.existsById(product.getId())).isTrue();
    }

    @Test
    void adminProductSearchAppliesAllFilters() {
        Product matching = ProductTestData.product("Admin Filter Match");
        matching.setProductCategory(ProductCategory.FIGURE);
        matching.setMaterial(PrintMaterial.PETG);
        productRepository.save(matching);
        productRepository.save(ProductTestData.product("Other Product"));

        List<ProductCatalogItemDto> result = productService.getAllProductsForAdmin(
                "filter",
                ProductCategory.FIGURE,
                PrintMaterial.PETG,
                matching.getPrice(),
                matching.getPrice(),
                true
        );

        assertThat(result)
                .singleElement()
                .extracting(ProductCatalogItemDto::getName)
                .isEqualTo("Admin Filter Match");
    }

    @Test
    void createAndUpdateRejectDuplicateProductNames() {
        Product existing = productRepository.save(ProductTestData.product("Existing Product"));
        Product other = productRepository.save(ProductTestData.product("Other Product"));

        assertThatThrownBy(() -> productService.createProduct(ProductTestData.productForm("Existing Product")))
                .isInstanceOf(ProductNameAlreadyExistsException.class);

        ProductFormDto duplicateUpdate = ProductTestData.productForm("Existing Product");
        assertThatThrownBy(() -> productService.updateProduct(other.getId(), duplicateUpdate))
                .isInstanceOf(ProductNameAlreadyExistsException.class);
        assertThat(productRepository.findById(existing.getId())).isPresent();
    }

    private Product withCreatedOn(Product product, LocalDateTime createdOn) {
        product.setCreatedOn(createdOn);
        return product;
    }
}
