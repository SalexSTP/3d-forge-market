package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.payment.InvoiceDownloadResult;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.payment.PaymentTransactionRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvoiceService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserRepository userRepository;
    private final CustomPrintRequestService customPrintRequestService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceService(
            PaymentTransactionRepository paymentTransactionRepository,
            CustomerOrderRepository customerOrderRepository,
            UserRepository userRepository,
            CustomPrintRequestService customPrintRequestService,
            InvoicePdfService invoicePdfService
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.userRepository = userRepository;
        this.customPrintRequestService = customPrintRequestService;
        this.invoicePdfService = invoicePdfService;
    }

    @Transactional
    public InvoiceDownloadResult downloadCustomerInvoice(UUID paymentTransactionId, UUID currentCustomerId) {
        PaymentTransaction payment = findPayment(paymentTransactionId);

        if (!payment.getCustomerId().equals(currentCustomerId)) {
            throw new PaymentOperationFailedException("You cannot download this invoice.");
        }

        return invoiceFor(payment);
    }

    @Transactional
    public InvoiceDownloadResult downloadAdminInvoice(UUID paymentTransactionId) {
        return invoiceFor(findPayment(paymentTransactionId));
    }

    private InvoiceDownloadResult invoiceFor(PaymentTransaction payment) {
        validateInvoiceStatus(payment);

        if (payment.getPaymentMethod() == PaymentMethod.STRIPE_CHECKOUT) {
            if (payment.getStripeInvoicePdfUrl() == null || payment.getStripeInvoicePdfUrl().isBlank()) {
                throw new PaymentOperationFailedException("Invoice is not available yet.");
            }

            return InvoiceDownloadResult.redirect(payment.getStripeInvoicePdfUrl());
        }

        if (payment.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            throw new PaymentOperationFailedException("Invoice is not available for this payment.");
        }

        User customer = userRepository.findById(payment.getCustomerId())
                .orElseThrow(() -> new PaymentOperationFailedException("Invoice is not available."));

        byte[] pdfBytes = switch (payment.getTargetType()) {
            case PRODUCT_ORDER -> {
                CustomerOrder order = findOrder(payment.getTargetId());
                validateCashTargetNotCancelledBeforePayment(payment, order);
                yield invoicePdfService.generateProductOrderInvoice(payment, customer, order);
            }
            case CUSTOM_PRINT_REQUEST -> invoicePdfService.generateCustomPrintInvoice(
                    payment,
                    customer,
                    findInvoiceableCustomPrint(payment)
            );
        };

        payment.setInvoiceGeneratedOn(LocalDateTime.now());
        paymentTransactionRepository.save(payment);

        return InvoiceDownloadResult.pdf(pdfBytes, "invoice-" + payment.getId() + ".pdf");
    }

    private PaymentTransaction findPayment(UUID paymentTransactionId) {
        return paymentTransactionRepository.findById(paymentTransactionId)
                .orElseThrow(() -> new PaymentOperationFailedException("Invoice is not available."));
    }

    private CustomerOrder findOrder(UUID orderId) {
        return customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentOperationFailedException("Invoice is not available."));
    }

    private CustomPrintRequestDetailsClientDto findCustomPrint(PaymentTransaction payment) {
        return customPrintRequestService.getRequestDetailsForAdmin(payment.getTargetId());
    }

    private CustomPrintRequestDetailsClientDto findInvoiceableCustomPrint(PaymentTransaction payment) {
        CustomPrintRequestDetailsClientDto request = findCustomPrint(payment);

        if (payment.getPaymentStatus() != PaymentStatus.PAID
                && (request.status() == CustomPrintRequestStatus.CANCELLED
                || request.status() == CustomPrintRequestStatus.REJECTED)) {
            throw new PaymentOperationFailedException("Invoice is not available for cancelled payments.");
        }

        return request;
    }

    private void validateCashTargetNotCancelledBeforePayment(PaymentTransaction payment, CustomerOrder order) {
        if (payment.getPaymentStatus() != PaymentStatus.PAID
                && order.getStatus() == OrderStatus.CANCELLED) {
            throw new PaymentOperationFailedException("Invoice is not available for cancelled payments.");
        }
    }

    private void validateInvoiceStatus(PaymentTransaction payment) {
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new PaymentOperationFailedException("Invoice is not available for cancelled payments.");
        }

        if (payment.getPaymentStatus() == PaymentStatus.FAILED
                || payment.getPaymentStatus() == PaymentStatus.PENDING) {
            throw new PaymentOperationFailedException("Invoice is not available until the payment is confirmed.");
        }

        if (payment.getPaymentStatus() != PaymentStatus.PAID
                && payment.getPaymentStatus() != PaymentStatus.PENDING_CASH_ON_DELIVERY) {
            throw new PaymentOperationFailedException("Invoice is not available.");
        }
    }
}
