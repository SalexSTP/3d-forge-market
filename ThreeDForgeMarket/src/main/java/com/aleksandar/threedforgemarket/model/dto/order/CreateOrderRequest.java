package com.aleksandar.threedforgemarket.model.dto.order;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {
    @NotNull(message = "Product is required.")
    private UUID productId;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 20, message = "Quantity cannot exceed 20.")
    private Integer quantity;

    @NotBlank(message = "Delivery address is required.")
    @Size(max = 255, message = "Delivery address must not exceed 255 characters.")
    private String deliveryAddress;

    @Size(max = 1000, message = "Order note must not exceed 1000 characters.")
    private String customerNote;
}
