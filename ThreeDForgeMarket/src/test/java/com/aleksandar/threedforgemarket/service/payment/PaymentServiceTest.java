package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.config.PaymentProperties;
import com.aleksandar.threedforgemarket.config.StripeProperties;
import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.exception.payment.StripePaymentUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.order.CreatedOrderDto;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentStartResult;
import com.aleksandar.threedforgemarket.model.dto.payment.StripeCancelResult;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.payment.PaymentTransactionRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.aleksandar.threedforgemarket.testdata.CustomPrintClientTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private StripeCheckoutClient stripeCheckoutClient;

    @Mock
    private CustomPrintRequestService customPrintRequestService;

    private UUID customerId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        lenient().when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> {
                    PaymentTransaction transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(UUID.randomUUID());
                    }
                    return transaction;
                });
    }

    @Test
    void cashOnDeliveryPaymentCreationForProductOrderPersistsPendingCashTransaction() {
        PaymentService paymentService = paymentService(false);
        CreatedOrderDto order = productOrder();

        PaymentStartResult result = paymentService.startProductOrderPayment(order, PaymentMethod.CASH_ON_DELIVERY);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertThat(result.redirectUrl()).isEqualTo("/orders/my");
        assertThat(captor.getValue().getTargetType()).isEqualTo(PaymentTargetType.PRODUCT_ORDER);
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING_CASH_ON_DELIVERY);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("42.50");
    }

    @Test
    void stripePaymentTransactionCreationCreatesCheckoutSessionAndStoresSessionId() throws Exception {
        PaymentService paymentService = paymentService(true);
        when(stripeCheckoutClient.createCheckoutSession(any()))
                .thenReturn(new StripeCheckoutResult("cs_test_123", "https://checkout.stripe.test/session"));

        PaymentStartResult result = paymentService.startProductOrderPayment(productOrder(), PaymentMethod.STRIPE_CHECKOUT);

        ArgumentCaptor<StripeCheckoutRequest> requestCaptor = ArgumentCaptor.forClass(StripeCheckoutRequest.class);
        verify(stripeCheckoutClient).createCheckoutSession(requestCaptor.capture());
        assertThat(result.externalRedirect()).isTrue();
        assertThat(result.redirectUrl()).isEqualTo("https://checkout.stripe.test/session");
        assertThat(requestCaptor.getValue().currency()).isEqualTo("eur");
        assertThat(requestCaptor.getValue().productName()).isEqualTo("Desk hook");
        assertThat(requestCaptor.getValue().productImageUrl()).isEqualTo("https://example.com/product.jpg");
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository, org.mockito.Mockito.times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStripeCheckoutSessionId()).isEqualTo("cs_test_123");
    }

    @Test
    void productStripeCheckoutOmitsNonPublicImageUrl() throws Exception {
        PaymentService paymentService = paymentService(true);
        when(stripeCheckoutClient.createCheckoutSession(any()))
                .thenReturn(new StripeCheckoutResult("cs_test_123", "https://checkout.stripe.test/session"));

        paymentService.startProductOrderPayment(
                productOrder("/images/local-product.jpg"),
                PaymentMethod.STRIPE_CHECKOUT
        );

        ArgumentCaptor<StripeCheckoutRequest> requestCaptor = ArgumentCaptor.forClass(StripeCheckoutRequest.class);
        verify(stripeCheckoutClient).createCheckoutSession(requestCaptor.capture());
        assertThat(requestCaptor.getValue().productImageUrl()).isNull();
    }

    @Test
    void stripeDisabledRejectsCheckoutWithoutCallingStripe() throws Exception {
        PaymentService paymentService = paymentService(false);

        assertThatThrownBy(() -> paymentService.startProductOrderPayment(productOrder(), PaymentMethod.STRIPE_CHECKOUT))
                .isInstanceOf(StripePaymentUnavailableException.class);
        verify(stripeCheckoutClient, never()).createCheckoutSession(any());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void customPrintStripeDisabledRejectsWithoutSavingPaymentOrAcceptingOffer() throws Exception {
        PaymentService paymentService = paymentService(false);
        when(customPrintRequestService.getCustomerRequestDetails(customerId, targetId))
                .thenReturn(CustomPrintClientTestData.details(targetId, customerId, CustomPrintRequestStatus.OFFER_SENT));

        assertThatThrownBy(() -> paymentService.startCustomPrintOfferPayment(
                customerId,
                targetId,
                PaymentMethod.STRIPE_CHECKOUT
        )).isInstanceOf(StripePaymentUnavailableException.class);

        verify(stripeCheckoutClient, never()).createCheckoutSession(any());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(customPrintRequestService, never()).acceptOffer(customerId, targetId);
    }

    @Test
    void completedWebhookMarksTransactionPaidAndAcceptsCustomPrintOffer() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.CUSTOM_PRINT_REQUEST, PaymentStatus.PENDING);
        when(paymentTransactionRepository.findByStripeCheckoutSessionId("cs_test_123"))
                .thenReturn(Optional.of(transaction));

        paymentService.handleCheckoutSessionCompleted(new StripeWebhookSession(
                "cs_test_123",
                "pi_test_123",
                Map.of("paymentTransactionId", transaction.getId().toString())
        ));

        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(transaction.getStripePaymentIntentId()).isEqualTo("pi_test_123");
        assertThat(transaction.getPaidOn()).isNotNull();
        verify(customPrintRequestService).acceptOffer(customerId, targetId);
    }

    @Test
    void expiredWebhookCancelsPendingStripePayment() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PENDING);
        when(paymentTransactionRepository.findByStripeCheckoutSessionId("cs_test_123"))
                .thenReturn(Optional.of(transaction));

        paymentService.handleCheckoutSessionExpired(new StripeWebhookSession("cs_test_123", null, Map.of()));

        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(transaction.getCancelledOn()).isNotNull();
    }

    @Test
    void customPrintStripePaymentDoesNotAcceptUntilWebhookConfirmation() throws Exception {
        PaymentService paymentService = paymentService(true);
        when(customPrintRequestService.getCustomerRequestDetails(customerId, targetId))
                .thenReturn(CustomPrintClientTestData.details(targetId, customerId, CustomPrintRequestStatus.OFFER_SENT));
        when(stripeCheckoutClient.createCheckoutSession(any()))
                .thenReturn(new StripeCheckoutResult("cs_test_123", "https://checkout.stripe.test/session"));

        paymentService.startCustomPrintOfferPayment(customerId, targetId, PaymentMethod.STRIPE_CHECKOUT);

        verify(customPrintRequestService, never()).acceptOffer(customerId, targetId);
    }

    @Test
    void customPrintStripeCheckoutUsesPrefixedProductName() throws Exception {
        PaymentService paymentService = paymentService(true);
        when(customPrintRequestService.getCustomerRequestDetails(customerId, targetId))
                .thenReturn(CustomPrintClientTestData.details(targetId, customerId, CustomPrintRequestStatus.OFFER_SENT));
        when(stripeCheckoutClient.createCheckoutSession(any()))
                .thenReturn(new StripeCheckoutResult("cs_test_123", "https://checkout.stripe.test/session"));

        paymentService.startCustomPrintOfferPayment(customerId, targetId, PaymentMethod.STRIPE_CHECKOUT);

        ArgumentCaptor<StripeCheckoutRequest> requestCaptor = ArgumentCaptor.forClass(StripeCheckoutRequest.class);
        verify(stripeCheckoutClient).createCheckoutSession(requestCaptor.capture());
        assertThat(requestCaptor.getValue().productName()).startsWith("Custom print: ");
        assertThat(requestCaptor.getValue().productImageUrl()).isNull();
    }

    @Test
    void cashOnDeliveryCustomPrintPaymentUsesQuotedPriceAndAcceptsImmediately() {
        PaymentService paymentService = paymentService(false);
        when(customPrintRequestService.getCustomerRequestDetails(customerId, targetId))
                .thenReturn(CustomPrintClientTestData.details(targetId, customerId, CustomPrintRequestStatus.OFFER_SENT));

        paymentService.startCustomPrintOfferPayment(customerId, targetId, PaymentMethod.CASH_ON_DELIVERY);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("35.00");
        verify(customPrintRequestService).acceptOffer(customerId, targetId);
    }

    @Test
    void deliveryMarksPendingCashPaymentPaid() {
        PaymentService paymentService = paymentService(false);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PENDING_CASH_ON_DELIVERY);
        transaction.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        when(paymentTransactionRepository.findFirstByTargetTypeAndTargetIdAndPaymentStatusOrderByCreatedOnDesc(
                PaymentTargetType.PRODUCT_ORDER,
                targetId,
                PaymentStatus.PENDING_CASH_ON_DELIVERY
        )).thenReturn(Optional.of(transaction));

        paymentService.markCashOnDeliveryPaidIfPending(PaymentTargetType.PRODUCT_ORDER, targetId);

        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(transaction.getPaidOn()).isNotNull();
    }

    @Test
    void cancelPendingStripeProductPaymentCancelsPaymentAndHidesOrder() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PENDING);
        CustomerOrder order = productOrderEntity(transaction.getTargetId(), customerId);
        when(paymentTransactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(customerOrderRepository.findByIdAndCustomer_Id(transaction.getTargetId(), customerId))
                .thenReturn(Optional.of(order));

        StripeCancelResult result = paymentService.cancelStripeCheckoutPayment(transaction.getId(), customerId);

        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(transaction.getCancelledOn()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.isDeletedFromCustomerHistory()).isTrue();
        assertThat(order.isDeletedFromAdminHistory()).isTrue();
        assertThat(result.productId()).isEqualTo(order.getProduct().getId());
        assertThat(result.orderForm().getQuantity()).isEqualTo(order.getQuantity());
        assertThat(result.orderForm().getDeliveryAddress()).isEqualTo(order.getDeliveryAddress());
        assertThat(result.orderForm().getCustomerNote()).isEqualTo(order.getCustomerNote());
        assertThat(result.orderForm().getPaymentMethod()).isEqualTo(PaymentMethod.STRIPE_CHECKOUT);
        verify(customerOrderRepository).save(order);
    }

    @Test
    void alreadyPaidStripePaymentCannotBeCancelled() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PAID);
        when(paymentTransactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> paymentService.cancelStripeCheckoutPayment(transaction.getId(), customerId))
                .isInstanceOf(PaymentOperationFailedException.class);
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void cashOnDeliveryPaymentCannotBeCancelledThroughStripeCancelFlow() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PENDING_CASH_ON_DELIVERY);
        transaction.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        when(paymentTransactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> paymentService.cancelStripeCheckoutPayment(transaction.getId(), customerId))
                .isInstanceOf(PaymentOperationFailedException.class);
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void customerCannotCancelAnotherCustomersStripePayment() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PENDING);
        when(paymentTransactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> paymentService.cancelStripeCheckoutPayment(transaction.getId(), UUID.randomUUID()))
                .isInstanceOf(PaymentOperationFailedException.class);
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void customPrintStripeCancelMarksPaymentCancelledWithoutAcceptingOffer() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.CUSTOM_PRINT_REQUEST, PaymentStatus.PENDING);
        when(paymentTransactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        StripeCancelResult result = paymentService.cancelStripeCheckoutPayment(transaction.getId(), customerId);

        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(transaction.getCancelledOn()).isNotNull();
        assertThat(result.customPrintRequestId()).isEqualTo(transaction.getTargetId());
        verify(customPrintRequestService, never()).acceptOffer(any(), any());
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void stripeSuccessResolutionDoesNotMarkPaymentPaid() {
        PaymentService paymentService = paymentService(true);
        PaymentTransaction transaction = transaction(PaymentTargetType.PRODUCT_ORDER, PaymentStatus.PENDING);
        when(paymentTransactionRepository.findByStripeCheckoutSessionId("cs_test_123"))
                .thenReturn(Optional.of(transaction));

        var result = paymentService.resolveStripeCheckoutSuccess("cs_test_123", customerId);

        assertThat(result.targetType()).isEqualTo(PaymentTargetType.PRODUCT_ORDER);
        assertThat(result.targetId()).isEqualTo(targetId);
        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(transaction.getPaidOn()).isNull();
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    private PaymentService paymentService(boolean stripeEnabled) {
        return new PaymentService(
                paymentTransactionRepository,
                customerOrderRepository,
                stripeCheckoutClient,
                new StripeProperties(stripeEnabled, stripeEnabled ? "sk_test_key" : "", "whsec_test", "3DForgeMarket"),
                new PaymentProperties("eur"),
                customPrintRequestService,
                "http://localhost"
        );
    }

    private CreatedOrderDto productOrder() {
        return productOrder("https://example.com/product.jpg");
    }

    private CreatedOrderDto productOrder(String imageUrl) {
        return new CreatedOrderDto(
                targetId,
                customerId,
                UUID.randomUUID(),
                "Desk hook",
                imageUrl,
                new BigDecimal("42.50")
        );
    }

    private PaymentTransaction transaction(PaymentTargetType targetType, PaymentStatus status) {
        return PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .targetType(targetType)
                .targetId(targetId)
                .amount(new BigDecimal("35.00"))
                .currency("eur")
                .paymentMethod(PaymentMethod.STRIPE_CHECKOUT)
                .paymentStatus(status)
                .stripeCheckoutSessionId("cs_test_123")
                .build();
    }

    private CustomerOrder productOrderEntity(UUID orderId, UUID customerId) {
        User customer = UserTestData.customer("stripe_customer");
        customer.setId(customerId);
        Product product = ProductTestData.product("Stripe Cancel Product");
        product.setId(UUID.randomUUID());

        return CustomerOrder.builder()
                .id(orderId)
                .customer(customer)
                .product(product)
                .quantity(3)
                .deliveryAddress("123 Stripe Street")
                .customerNote("Leave near reception")
                .totalPrice(new BigDecimal("75.00"))
                .status(OrderStatus.PENDING)
                .build();
    }
}
