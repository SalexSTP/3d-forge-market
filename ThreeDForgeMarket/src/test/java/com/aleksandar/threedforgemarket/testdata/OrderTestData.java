package com.aleksandar.threedforgemarket.testdata;

import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public final class OrderTestData {

    private OrderTestData() {
    }

    public static CreateOrderRequest createOrderRequest(UUID productId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(productId);
        request.setQuantity(2);
        request.setDeliveryAddress("123 Test Street");
        request.setCustomerNote("Please leave at reception.");
        return request;
    }

    public static CustomerOrder order(User customer, Product product, OrderStatus status) {
        return CustomerOrder.builder()
                .customer(customer)
                .product(product)
                .quantity(1)
                .deliveryAddress("123 Test Street")
                .customerNote("Short note")
                .totalPrice(new BigDecimal("12.50"))
                .status(status)
                .deletedFromCustomerHistory(false)
                .deletedFromAdminHistory(false)
                .build();
    }
}
