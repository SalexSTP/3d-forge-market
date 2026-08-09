package com.aleksandar.threedforgemarket.repository.payment;

import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    Optional<PaymentTransaction> findFirstByTargetTypeAndTargetIdOrderByCreatedOnDesc(
            PaymentTargetType targetType,
            UUID targetId
    );

    List<PaymentTransaction> findByCustomerIdOrderByCreatedOnDesc(UUID customerId);

    Optional<PaymentTransaction> findFirstByTargetTypeAndTargetIdAndPaymentStatusOrderByCreatedOnDesc(
            PaymentTargetType targetType,
            UUID targetId,
            PaymentStatus paymentStatus
    );
}
