package com.aleksandar.threedforgemarket.web.controller.product;

import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductRoutesMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CacheManager cacheManager;

    private User customer;
    private User admin;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

        customer = userRepository.save(UserTestData.customer("product_customer"));
        admin = userRepository.save(UserTestData.admin("product_admin"));
        product = productRepository.save(ProductTestData.product("Public MVC Product"));
    }

    @Test
    void homePageOpens() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void publicProductCatalogOpens() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/catalog"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public MVC Product")));
    }

    @Test
    void productDetailsOpensForAvailableProduct() throws Exception {
        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("product/details"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public MVC Product")));
    }

    @Test
    void adminProductPagesRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/products").with(customer(customer.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/products").with(admin(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"));
    }
}
