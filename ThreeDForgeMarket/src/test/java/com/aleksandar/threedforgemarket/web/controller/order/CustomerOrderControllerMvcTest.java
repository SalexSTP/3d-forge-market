package com.aleksandar.threedforgemarket.web.controller.order;

import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerOrderControllerMvcTest {

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
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("mvc_customer"));
        otherCustomer = userRepository.save(UserTestData.customer("mvc_other"));
        product = productRepository.save(ProductTestData.product("MVC Order Product"));
    }

    @Test
    void unauthenticatedUserIsRedirectedToLoginForOrderPages() throws Exception {
        mockMvc.perform(get("/orders/my"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
    }

    @Test
    void customerCanOpenOrderCreationPageForAvailableProduct() throws Exception {
        mockMvc.perform(get("/orders/create")
                        .param("productId", product.getId().toString())
                        .with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("order/create"))
                .andExpect(model().attributeExists("product", "orderForm", "calculatedTotal"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirm your print order")));
    }

    @Test
    void customerCanSubmitValidOrderWithCsrf() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("quantity", "2")
                        .param("deliveryAddress", "123 MVC Street")
                        .param("customerNote", "Leave at door"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/my"));

        assertThat(customerOrderRepository.findAll())
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getCustomer().getId()).isEqualTo(customer.getId());
                    assertThat(order.getProduct().getId()).isEqualTo(product.getId());
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                });
    }

    @Test
    void invalidOrderSubmissionReturnsFormWithValidationErrors() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("quantity", "0")
                        .param("deliveryAddress", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("order/create"))
                .andExpect(model().attributeHasFieldErrors("orderForm", "quantity", "deliveryAddress"));
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(customer(customer.getId()))
                        .param("productId", product.getId().toString())
                        .param("quantity", "1")
                        .param("deliveryAddress", "123 MVC Street"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerSeesTheirOrderList() throws Exception {
        customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));
        customerOrderRepository.save(OrderTestData.order(otherCustomer, product, OrderStatus.PENDING));

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("order/my-orders"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MVC Order Product")));
    }

    @Test
    void customerCanCancelAllowedOrderWithCsrf() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));

        mockMvc.perform(put("/orders/{id}/cancel", order.getId())
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/my"));

        assertThat(customerOrderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void customerCannotCancelAnotherCustomersOrder() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(otherCustomer, product, OrderStatus.PENDING));

        mockMvc.perform(put("/orders/{id}/cancel", order.getId())
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/my"));

        assertThat(customerOrderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void customerCanRemoveEligibleOrderFromHistory() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.DELIVERED));

        mockMvc.perform(delete("/orders/{id}", order.getId())
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/my"));

        CustomerOrder updated = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.isDeletedFromCustomerHistory()).isTrue();
        assertThat(updated.isDeletedFromAdminHistory()).isFalse();
    }
}
