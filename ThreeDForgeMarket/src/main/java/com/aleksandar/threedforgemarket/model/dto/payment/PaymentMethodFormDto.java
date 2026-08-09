package com.aleksandar.threedforgemarket.model.dto.payment;

import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethodFormDto {

    @NotNull(message = "Please choose a payment method.")
    private PaymentMethod paymentMethod;
}
