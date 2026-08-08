package com.aleksandar.threedforgemarket.service.order;

import com.aleksandar.threedforgemarket.exception.order.OrderCancellationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderStatusUpdateNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.ProductUnavailableException;
import com.aleksandar.threedforgemarket.model.dto.order.AdminOrderListItemDto;
import com.aleksandar.threedforgemarket.model.dto.order.CustomerOrderListItemDto;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CustomerOrderServiceTest {

    @Autowired
    private CustomerOrderService customerOrderService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User customer;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("customer"));
        product = productRepository.save(ProductTestData.product("Order Product"));
    }

    @Test
    void createOrderFromAvailableProductPersistsPendingOrder() {
        customerOrderService.createOrder(customer.getId(), OrderTestData.createOrderRequest(product.getId()));

        assertThat(customerOrderRepository.findAll())
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getCustomer().getId()).isEqualTo(customer.getId());
                    assertThat(order.getProduct().getId()).isEqualTo(product.getId());
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.getTotalPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
                });
    }

    @Test
    void createOrderRejectsUnavailableProductAndNonCustomerUser() {
        product.setAvailable(false);
        productRepository.save(product);
        User admin = userRepository.save(UserTestData.admin("admin"));

        assertThatThrownBy(() -> customerOrderService.createOrder(customer.getId(), OrderTestData.createOrderRequest(product.getId())))
                .isInstanceOf(ProductUnavailableException.class);
        assertThatThrownBy(() -> customerOrderService.createOrder(admin.getId(), OrderTestData.createOrderRequest(product.getId())))
                .isInstanceOf(OrderCreationNotAllowedException.class);
    }

    @Test
    void customerOrderListReturnsOnlyVisibleOrdersForCurrentCustomer() {
        User otherCustomer = userRepository.save(UserTestData.customer("other"));
        CustomerOrder visible = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));
        CustomerOrder hidden = OrderTestData.order(customer, product, OrderStatus.CANCELLED);
        hidden.setDeletedFromCustomerHistory(true);
        customerOrderRepository.save(hidden);
        customerOrderRepository.save(OrderTestData.order(otherCustomer, product, OrderStatus.PENDING));

        List<CustomerOrderListItemDto> result = customerOrderService.getOrdersForCustomer(customer.getId());

        assertThat(result)
                .singleElement()
                .extracting(CustomerOrderListItemDto::getId)
                .isEqualTo(visible.getId());
    }

    @Test
    void cancelOrderAllowedOnlyForPendingOrConfirmedOrders() {
        CustomerOrder pending = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));
        CustomerOrder delivered = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.DELIVERED));

        customerOrderService.cancelOrder(customer.getId(), pending.getId());

        assertThat(customerOrderRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> customerOrderService.cancelOrder(customer.getId(), delivered.getId()))
                .isInstanceOf(OrderCancellationNotAllowedException.class);
    }

    @Test
    void customerHistoryRemovalOnlyHidesFromCustomer() {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.DELIVERED));

        customerOrderService.deleteOrderFromHistory(customer.getId(), order.getId());

        CustomerOrder updated = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.isDeletedFromCustomerHistory()).isTrue();
        assertThat(updated.isDeletedFromAdminHistory()).isFalse();
        assertThatThrownBy(() -> customerOrderService.deleteOrderFromHistory(customer.getId(), customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING)).getId()))
                .isInstanceOf(OrderDeletionNotAllowedException.class);
    }

    @Test
    void adminStatusUpdateFollowsAllowedWorkflow() {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));

        customerOrderService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);

        assertThat(customerOrderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> customerOrderService.updateOrderStatus(order.getId(), OrderStatus.DELIVERED))
                .isInstanceOf(OrderStatusUpdateNotAllowedException.class);
    }

    @Test
    void adminHistoryRemovalOnlyHidesFromAdmin() {
        CustomerOrder order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.CANCELLED));

        customerOrderService.deleteOrderFromAdminHistory(order.getId());

        CustomerOrder updated = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.isDeletedFromAdminHistory()).isTrue();
        assertThat(updated.isDeletedFromCustomerHistory()).isFalse();
        List<AdminOrderListItemDto> adminOrders = customerOrderService.getAllOrdersForAdmin();
        assertThat(adminOrders).extracting(AdminOrderListItemDto::getId).doesNotContain(order.getId());
    }
}
