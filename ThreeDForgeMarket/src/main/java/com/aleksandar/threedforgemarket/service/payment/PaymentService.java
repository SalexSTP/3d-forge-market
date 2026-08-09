package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.config.PaymentProperties;
import com.aleksandar.threedforgemarket.config.StripeProperties;
import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.exception.payment.StripePaymentUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.order.CreatedOrderDto;
import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentStartResult;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentSummaryDto;
import com.aleksandar.threedforgemarket.model.dto.payment.StripeCancelResult;
import com.aleksandar.threedforgemarket.model.dto.payment.StripeSuccessResult;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.payment.PaymentTransactionRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);
    private static final String EUR = "eur";

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final StripeCheckoutClient stripeCheckoutClient;
    private final StripeInvoiceClient stripeInvoiceClient;
    private final StripeProperties stripeProperties;
    private final PaymentProperties paymentProperties;
    private final CustomPrintRequestService customPrintRequestService;
    private final String appBaseUrl;

    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            CustomerOrderRepository customerOrderRepository,
            StripeCheckoutClient stripeCheckoutClient,
            StripeInvoiceClient stripeInvoiceClient,
            StripeProperties stripeProperties,
            PaymentProperties paymentProperties,
            CustomPrintRequestService customPrintRequestService,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.stripeCheckoutClient = stripeCheckoutClient;
        this.stripeInvoiceClient = stripeInvoiceClient;
        this.stripeProperties = stripeProperties;
        this.paymentProperties = paymentProperties;
        this.customPrintRequestService = customPrintRequestService;
        this.appBaseUrl = trimTrailingSlash(appBaseUrl);
    }

    public boolean isStripeCheckoutAvailable() {
        return stripeProperties.isCheckoutConfigured();
    }

    @Transactional
    public PaymentStartResult startProductOrderPayment(CreatedOrderDto order, PaymentMethod paymentMethod) {
        validatePaymentMethod(paymentMethod);

        if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
            createTransaction(
                    order.customerId(),
                    PaymentTargetType.PRODUCT_ORDER,
                    order.id(),
                    order.totalPrice(),
                    paymentMethod,
                    PaymentStatus.PENDING_CASH_ON_DELIVERY
            );
            LOGGER.info("Cash payment transaction created for product order id={}", order.id());
            return PaymentStartResult.local("/orders/my");
        }

        ensureStripeCheckoutConfigured();

        PaymentTransaction transaction = createTransaction(
                order.customerId(),
                PaymentTargetType.PRODUCT_ORDER,
                order.id(),
                order.totalPrice(),
                paymentMethod,
                PaymentStatus.PENDING
        );

        StripeCheckoutResult checkoutResult = createStripeCheckoutSession(
                transaction,
                safeName(order.productName(), "3DForgeMarket order"),
                publicImageUrl(order.productImageUrl())
        );

        transaction.setStripeCheckoutSessionId(checkoutResult.sessionId());
        paymentTransactionRepository.save(transaction);
        LOGGER.info("Stripe Checkout Session created for product order id={}", order.id());

        return PaymentStartResult.external(checkoutResult.checkoutUrl());
    }

    @Transactional
    public PaymentStartResult startCustomPrintOfferPayment(
            UUID customerId,
            UUID requestId,
            PaymentMethod paymentMethod
    ) {
        validatePaymentMethod(paymentMethod);

        CustomPrintRequestDetailsClientDto request = customPrintRequestService.getCustomerRequestDetails(customerId, requestId);
        validatePayableCustomPrintOffer(request);

        if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
            createTransaction(
                    customerId,
                    PaymentTargetType.CUSTOM_PRINT_REQUEST,
                    requestId,
                    request.quotedPrice(),
                    paymentMethod,
                    PaymentStatus.PENDING_CASH_ON_DELIVERY
            );
            customPrintRequestService.acceptOffer(customerId, requestId);
            LOGGER.info("Cash payment transaction created and custom print offer accepted for request id={}", requestId);
            return PaymentStartResult.local("/custom-prints/" + requestId);
        }

        ensureStripeCheckoutConfigured();

        PaymentTransaction transaction = createTransaction(
                customerId,
                PaymentTargetType.CUSTOM_PRINT_REQUEST,
                requestId,
                request.quotedPrice(),
                paymentMethod,
                PaymentStatus.PENDING
        );

        StripeCheckoutResult checkoutResult = createStripeCheckoutSession(
                transaction,
                customPrintProductName(request.title()),
                null
        );

        transaction.setStripeCheckoutSessionId(checkoutResult.sessionId());
        paymentTransactionRepository.save(transaction);
        LOGGER.info("Stripe Checkout Session created for custom print request id={}", requestId);

        return PaymentStartResult.external(checkoutResult.checkoutUrl());
    }

    @Transactional
    public void handleCheckoutSessionCompleted(StripeWebhookSession session) {
        PaymentTransaction transaction = findWebhookTransaction(session);

        if (transaction.getPaymentStatus() == PaymentStatus.PAID) {
            if (transaction.getStripeInvoicePdfUrl() == null || transaction.getStripeInvoicePdfUrl().isBlank()) {
                storeStripeInvoiceData(transaction, session.invoiceId());
                paymentTransactionRepository.save(transaction);
            }
            return;
        }

        transaction.setPaymentStatus(PaymentStatus.PAID);
        transaction.setStripePaymentIntentId(session.paymentIntentId());
        transaction.setPaidOn(LocalDateTime.now());
        storeStripeInvoiceData(transaction, session.invoiceId());
        paymentTransactionRepository.save(transaction);
        LOGGER.info("Stripe payment confirmed by webhook for transaction id={}", transaction.getId());

        if (transaction.getTargetType() == PaymentTargetType.CUSTOM_PRINT_REQUEST) {
            customPrintRequestService.acceptOffer(transaction.getCustomerId(), transaction.getTargetId());
            LOGGER.info("Custom print offer accepted after payment for request id={}", transaction.getTargetId());
        }
    }

    @Transactional
    public void handleCheckoutSessionExpired(StripeWebhookSession session) {
        PaymentTransaction transaction = findWebhookTransaction(session);

        if (transaction.getPaymentStatus() == PaymentStatus.PENDING) {
            transaction.setPaymentStatus(PaymentStatus.CANCELLED);
            transaction.setCancelledOn(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);
            LOGGER.info("Stripe payment cancelled or expired for transaction id={}", transaction.getId());
        }
    }

    public StripeWebhookSession verifyWebhookEvent(
            String payload,
            String signatureHeader,
            String expectedEventType
    ) throws SignatureVerificationException {
        if (!stripeProperties.isWebhookConfigured()) {
            throw new SignatureVerificationException("Stripe webhook is not configured.", signatureHeader);
        }

        return stripeCheckoutClient.verifyCheckoutSessionEvent(payload, signatureHeader, expectedEventType);
    }

    @Transactional(readOnly = true)
    public StripeSuccessResult resolveStripeCheckoutSuccess(String sessionId, UUID currentCustomerId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByStripeCheckoutSessionId(sessionId)
                .orElseThrow(() -> new PaymentOperationFailedException("This payment can no longer be completed."));

        if (!transaction.getCustomerId().equals(currentCustomerId)) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        if (transaction.getPaymentMethod() != PaymentMethod.STRIPE_CHECKOUT) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        return new StripeSuccessResult(transaction.getTargetType(), transaction.getTargetId());
    }

    @Transactional
    public void markCashOnDeliveryPaidIfPending(PaymentTargetType targetType, UUID targetId) {
        paymentTransactionRepository
                .findFirstByTargetTypeAndTargetIdAndPaymentStatusOrderByCreatedOnDesc(
                        targetType,
                        targetId,
                        PaymentStatus.PENDING_CASH_ON_DELIVERY
                )
                .filter(transaction -> transaction.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY)
                .ifPresent(transaction -> {
                    transaction.setPaymentStatus(PaymentStatus.PAID);
                    transaction.setPaidOn(LocalDateTime.now());
                    paymentTransactionRepository.save(transaction);
                    LOGGER.info("Cash-on-delivery payment marked paid for {} id={}", targetType, targetId);
                });
    }

    @Transactional(readOnly = true)
    public Optional<PaymentSummaryDto> getLatestPaymentSummary(PaymentTargetType targetType, UUID targetId) {
        return getLatestPaymentSummary(
                targetType,
                targetId,
                isTargetCancelledBeforePayment(targetType, targetId)
        );
    }

    @Transactional(readOnly = true)
    public Optional<PaymentSummaryDto> getLatestPaymentSummary(
            PaymentTargetType targetType,
            UUID targetId,
            boolean targetCancelledBeforePayment
    ) {
        return paymentTransactionRepository.findFirstByTargetTypeAndTargetIdOrderByCreatedOnDesc(targetType, targetId)
                .map(transaction -> PaymentSummaryDto.builder()
                        .paymentMethod(transaction.getPaymentMethod())
                        .paymentStatus(transaction.getPaymentStatus())
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .paymentTransactionId(transaction.getId())
                        .invoiceAvailable(isInvoiceAvailable(transaction, targetCancelledBeforePayment))
                        .stripeInvoice(transaction.getPaymentMethod() == PaymentMethod.STRIPE_CHECKOUT)
                        .build());
    }

    private void storeStripeInvoiceData(PaymentTransaction transaction, String stripeInvoiceId) {
        if (stripeInvoiceId == null || stripeInvoiceId.isBlank()) {
            return;
        }

        transaction.setStripeInvoiceId(stripeInvoiceId);

        try {
            stripeInvoiceClient.getInvoicePdfUrl(stripeInvoiceId)
                    .ifPresentOrElse(invoicePdfUrl -> {
                        transaction.setStripeInvoicePdfUrl(invoicePdfUrl);
                        transaction.setInvoiceGeneratedOn(LocalDateTime.now());
                    }, () -> LOGGER.warn(
                            "Stripe invoice PDF URL is not available yet for transaction id={} invoice id={}",
                            transaction.getId(),
                            stripeInvoiceId
                    ));
        } catch (StripeException exception) {
            LOGGER.warn(
                    "Could not retrieve Stripe invoice PDF URL for transaction id={} invoice id={}",
                    transaction.getId(),
                    stripeInvoiceId
            );
        }
    }

    private boolean isInvoiceAvailable(PaymentTransaction transaction, boolean targetCancelledBeforePayment) {
        if (transaction.getPaymentMethod() == PaymentMethod.STRIPE_CHECKOUT) {
            return transaction.getPaymentStatus() == PaymentStatus.PAID
                    && transaction.getStripeInvoicePdfUrl() != null
                    && !transaction.getStripeInvoicePdfUrl().isBlank();
        }

        if (transaction.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            return false;
        }

        if (transaction.getPaymentStatus() == PaymentStatus.PAID) {
            return true;
        }

        return transaction.getPaymentStatus() == PaymentStatus.PENDING_CASH_ON_DELIVERY
                && !targetCancelledBeforePayment;
    }

    private boolean isTargetCancelledBeforePayment(PaymentTargetType targetType, UUID targetId) {
        if (targetType == PaymentTargetType.PRODUCT_ORDER) {
            return customerOrderRepository.findById(targetId)
                    .map(order -> order.getStatus() == OrderStatus.CANCELLED)
                    .orElse(false);
        }

        try {
            CustomPrintRequestDetailsClientDto request = customPrintRequestService.getRequestDetailsForAdmin(targetId);
            return request.status() == CustomPrintRequestStatus.CANCELLED
                    || request.status() == CustomPrintRequestStatus.REJECTED;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private PaymentTransaction createTransaction(
            UUID customerId,
            PaymentTargetType targetType,
            UUID targetId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus
    ) {
        validateAmount(amount);
        validateCurrency();

        return paymentTransactionRepository.save(PaymentTransaction.builder()
                .customerId(customerId)
                .targetType(targetType)
                .targetId(targetId)
                .amount(amount)
                .currency(EUR)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .build());
    }

    private StripeCheckoutResult createStripeCheckoutSession(
            PaymentTransaction transaction,
            String productName,
            String productImageUrl
    ) {
        try {
            return stripeCheckoutClient.createCheckoutSession(new StripeCheckoutRequest(
                    transaction.getId(),
                    transaction.getCustomerId(),
                    transaction.getTargetType(),
                    transaction.getTargetId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    productName,
                    productImageUrl,
                    appBaseUrl + "/payments/stripe/success?session_id={CHECKOUT_SESSION_ID}",
                    appBaseUrl + "/payments/stripe/cancel?paymentTransactionId=" + transaction.getId()
            ));
        } catch (StripeException | ArithmeticException exception) {
            transaction.setPaymentStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            throw new PaymentOperationFailedException("Online payments are currently unavailable.");
        }
    }

    private void ensureStripeCheckoutConfigured() {
        if (!stripeProperties.isCheckoutConfigured()) {
            throw new StripePaymentUnavailableException();
        }
    }

    @Transactional
    public StripeCancelResult cancelStripeCheckoutPayment(UUID paymentTransactionId, UUID currentCustomerId) {
        if (paymentTransactionId == null) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findById(paymentTransactionId)
                .orElseThrow(() -> new PaymentOperationFailedException("This payment can no longer be completed."));

        if (!transaction.getCustomerId().equals(currentCustomerId)) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        if (transaction.getPaymentMethod() != PaymentMethod.STRIPE_CHECKOUT) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        if (transaction.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        transaction.setPaymentStatus(PaymentStatus.CANCELLED);
        transaction.setCancelledOn(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        if (transaction.getTargetType() == PaymentTargetType.PRODUCT_ORDER) {
            StripeCancelResult result = cancelUnpaidStripeProductOrder(transaction, currentCustomerId);
            LOGGER.info("Stripe Checkout cancelled and product order cleaned up for payment id={}", transaction.getId());
            return result;
        }

        LOGGER.info("Stripe Checkout cancelled for custom print payment id={}", transaction.getId());
        return StripeCancelResult.customPrintRequest(transaction.getTargetId());
    }

    private StripeCancelResult cancelUnpaidStripeProductOrder(
            PaymentTransaction transaction,
            UUID currentCustomerId
    ) {
        CustomerOrder order = customerOrderRepository
                .findByIdAndCustomer_Id(transaction.getTargetId(), currentCustomerId)
                .orElseThrow(() -> new PaymentOperationFailedException("This payment can no longer be completed."));

        CreateOrderRequest orderForm = new CreateOrderRequest();
        orderForm.setProductId(order.getProduct().getId());
        orderForm.setQuantity(order.getQuantity());
        orderForm.setDeliveryAddress(order.getDeliveryAddress());
        orderForm.setCustomerNote(order.getCustomerNote());
        orderForm.setPaymentMethod(isStripeCheckoutAvailable()
                ? PaymentMethod.STRIPE_CHECKOUT
                : PaymentMethod.CASH_ON_DELIVERY);

        order.setStatus(OrderStatus.CANCELLED);
        order.setDeletedFromCustomerHistory(true);
        order.setDeletedFromAdminHistory(true);
        customerOrderRepository.save(order);

        return StripeCancelResult.productOrder(order.getProduct().getId(), orderForm);
    }

    private PaymentTransaction findWebhookTransaction(StripeWebhookSession session) {
        if (session == null) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }

        return paymentTransactionRepository.findByStripeCheckoutSessionId(session.id())
                .or(() -> findByMetadataTransactionId(session))
                .orElseThrow(() -> new PaymentOperationFailedException("This payment can no longer be completed."));
    }

    private Optional<PaymentTransaction> findByMetadataTransactionId(StripeWebhookSession session) {
        String transactionId = session.metadata().get("paymentTransactionId");

        if (transactionId == null || transactionId.isBlank()) {
            return Optional.empty();
        }

        try {
            return paymentTransactionRepository.findById(UUID.fromString(transactionId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new PaymentOperationFailedException("Please choose a payment method.");
        }
    }

    private void validatePayableCustomPrintOffer(CustomPrintRequestDetailsClientDto request) {
        if (request.status() != CustomPrintRequestStatus.OFFER_SENT) {
            throw new PaymentOperationFailedException("Only offers waiting for your response can be paid.");
        }

        validateAmount(request.quotedPrice());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }
    }

    private void validateCurrency() {
        if (!EUR.equals(paymentProperties.currency())) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }
    }

    private String safeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String stripped = value.strip();
        return stripped.length() > 80 ? stripped.substring(0, 80) : stripped;
    }

    private String customPrintProductName(String title) {
        return safeName(
                title == null || title.isBlank()
                        ? null
                        : "Custom print: " + title.strip(),
                "Custom print request"
        );
    }

    private String publicImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String stripped = imageUrl.strip();
        return stripped.startsWith("http://") || stripped.startsWith("https://")
                ? stripped
                : null;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }

        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
