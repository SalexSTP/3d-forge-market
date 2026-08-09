package com.aleksandar.threedforgemarket.model.dto.payment;

import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
public class PaymentSummaryDto {
    private UUID paymentTransactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String currency;
    private boolean invoiceAvailable;
    private boolean stripeInvoice;
}
