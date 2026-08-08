package com.aleksandar.threedforgemarket.web.controller.review;

import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.Review;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.OrderTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.ReviewTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User customer;
    private User otherCustomer;
    private User admin;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("review_customer"));
        otherCustomer = userRepository.save(UserTestData.customer("other_review_customer"));
        admin = userRepository.save(UserTestData.admin("review_admin"));
        product = productRepository.save(ProductTestData.product("Review MVC Product"));
        CustomerOrder delivered = OrderTestData.order(customer, product, OrderStatus.DELIVERED);
        customerOrderRepository.save(delivered);
    }

    @Test
    void authenticatedCustomerCanSubmitReviewWithCsrf() throws Exception {
        mockMvc.perform(post("/reviews")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("rating", "5")
                        .param("comment", "This product printed very well."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/" + product.getId()));

        assertThat(reviewRepository.findAll())
                .singleElement()
                .extracting(Review::getRating)
                .isEqualTo(5);
    }

    @Test
    void invalidReviewReturnsFormWithValidationErrors() throws Exception {
        mockMvc.perform(post("/reviews")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("rating", "9")
                        .param("comment", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("review/create"));
    }

    @Test
    void reviewPostWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/reviews")
                        .with(customer(customer.getId()))
                        .param("productId", product.getId().toString())
                        .param("rating", "5")
                        .param("comment", "This product printed very well."))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotEditOrDeleteAnotherCustomersReview() throws Exception {
        Review review = reviewRepository.save(ReviewTestData.review(otherCustomer, product));

        mockMvc.perform(put("/reviews/{id}", review.getId())
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("rating", "4")
                        .param("comment", "Updated but not allowed."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(delete("/reviews/{id}", review.getId())
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        assertThat(reviewRepository.existsById(review.getId())).isTrue();
    }

    @Test
    void adminCanDeleteReview() throws Exception {
        Review review = reviewRepository.save(ReviewTestData.review(customer, product));

        mockMvc.perform(delete("/admin/reviews/{id}", review.getId())
                        .with(admin(admin.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reviews"));

        assertThat(reviewRepository.existsById(review.getId())).isFalse();
    }
}
