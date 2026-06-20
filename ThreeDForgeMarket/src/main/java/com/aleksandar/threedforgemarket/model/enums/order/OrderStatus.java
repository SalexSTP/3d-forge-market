package com.aleksandar.threedforgemarket.model.enums.order;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PRINTING("Printing"),
    READY_FOR_DELIVERY("Ready For Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),;

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
}
