package com.aleksandar.threedforgemarket.mapper.order;

import com.aleksandar.threedforgemarket.model.dto.order.AdminOrderListItemDto;
import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.dto.order.CustomerOrderListItemDto;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

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
            boolean cancellable,
            boolean deletable
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
                .deletable(deletable)
                .build();
    }

    public AdminOrderListItemDto toAdminListItemDto(
            CustomerOrder customerOrder,
            List<OrderStatus> availableStatusUpdates,
            boolean deletable
    ) {
        return AdminOrderListItemDto.builder()
                .id(customerOrder.getId())
                .customerUsername(customerOrder.getCustomer().getUsername())
                .customerEmail(customerOrder.getCustomer().getEmail())
                .productId(customerOrder.getProduct().getId())
                .productName(customerOrder.getProduct().getName())
                .quantity(customerOrder.getQuantity())
                .totalPrice(customerOrder.getTotalPrice())
                .deliveryAddress(customerOrder.getDeliveryAddress())
                .customerNote(customerOrder.getCustomerNote())
                .createdOn(customerOrder.getCreatedOn())
                .status(customerOrder.getStatus())
                .availableStatusUpdates(availableStatusUpdates)
                .deletable(deletable)
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}
