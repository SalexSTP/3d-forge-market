package com.aleksandar.threedforgemarket.mapper;

import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.dto.order.CustomerOrderListItemDto;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CustomerOrderMapper {

    public CustomerOrder toEntity(
            CreateOrderRequest orderRequest,
            Product product,
            User customer,
            BigDecimal totalPrice
    ) {
        return CustomerOrder.builder()
                .product(product)
                .customer(customer)
                .quantity(orderRequest.getQuantity())
                .deliveryAddress(orderRequest.getDeliveryAddress().strip())
                .customerNote(normalizeOptionalText(orderRequest.getCustomerNote()))
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .build();
    }

    public CustomerOrderListItemDto toListItemDto(
            CustomerOrder customerOrder,
            boolean cancellable
    ) {
        return CustomerOrderListItemDto.builder()
                .id(customerOrder.getId())
                .productId(customerOrder.getProduct().getId())
                .productName(customerOrder.getProduct().getName())
                .quantity(customerOrder.getQuantity())
                .totalPrice(customerOrder.getTotalPrice())
                .status(customerOrder.getStatus())
                .createdOn(customerOrder.getCreatedOn())
                .deliveryAddress(customerOrder.getDeliveryAddress())
                .customerNote(customerOrder.getCustomerNote())
                .cancellable(cancellable)
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}
