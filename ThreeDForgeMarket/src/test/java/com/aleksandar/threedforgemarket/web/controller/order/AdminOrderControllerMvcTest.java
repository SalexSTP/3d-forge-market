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

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOrderControllerMvcTest {

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

    private User admin;
    private User customer;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(UserTestData.admin("mvc_admin"));
        customer = userRepository.save(UserTestData.customer("mvc_customer"));
        product = productRepository.save(ProductTestData.product("Admin Order Product"));
    }

    @Test
    void customerCannotAccessAdminOrderPages() throws Exception {
        mockMvc.perform(get("/admin/orders").with(customer(customer.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessOrderList() throws Exception {
        customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));

        mockMvc.perform(get("/admin/orders").with(admin(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Order Product")));
    }

    @Test
    void adminCanUpdateOrderStatusWithCsrf() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));

        mockMvc.perform(put("/admin/orders/{id}/status", order.getId())
                        .with(admin(admin.getId()))
                        .with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));

        assertThat(customerOrderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void invalidStatusUpdateFailsGracefully() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));

        mockMvc.perform(put("/admin/orders/{id}/status", order.getId())
                        .with(admin(admin.getId()))
                        .with(csrf())
                        .param("status", "DELIVERED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));

        assertThat(customerOrderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void putWithoutCsrfIsRejected() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));

        mockMvc.perform(put("/admin/orders/{id}/status", order.getId())
                        .with(admin(admin.getId()))
                        .param("status", "CONFIRMED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRemoveEligibleOrderFromAdminHistory() throws Exception {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.CANCELLED));

        mockMvc.perform(delete("/admin/orders/{id}", order.getId())
                        .with(admin(admin.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));

        CustomerOrder updated = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.isDeletedFromAdminHistory()).isTrue();
        assertThat(updated.isDeletedFromCustomerHistory()).isFalse();
    }
}
