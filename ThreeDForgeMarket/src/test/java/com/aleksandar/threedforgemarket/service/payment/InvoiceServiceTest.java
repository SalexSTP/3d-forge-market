package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.model.dto.payment.InvoiceDownloadResult;
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
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.aleksandar.threedforgemarket.testdata.CustomPrintClientTestData;
import com.aleksandar.threedforgemarket.testdata.OrderTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomPrintRequestService customPrintRequestService;

    @Mock
    private InvoicePdfService invoicePdfService;

    private InvoiceService invoiceService;
    private UUID customerId;
    private UUID paymentId;
    private UUID targetId;
    private User customer;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                paymentTransactionRepository,
                customerOrderRepository,
                userRepository,
                customPrintRequestService,
                invoicePdfService
        );
        customerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        customer = UserTestData.customer("invoice_customer");
        customer.setId(customerId);
    }

    @Test
    void customerCanDownloadOwnCashInvoice() {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);
        CustomerOrder order = order();
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.findById(targetId)).thenReturn(Optional.of(order));
        when(invoicePdfService.generateProductOrderInvoice(payment, customer, order)).thenReturn("%PDF".getBytes());

        InvoiceDownloadResult result = invoiceService.downloadCustomerInvoice(paymentId, customerId);

        assertThat(result.type()).isEqualTo(InvoiceDownloadResult.Type.PDF);
        assertThat(result.pdfBytes()).containsExactly("%PDF".getBytes());
        assertThat(payment.getInvoiceGeneratedOn()).isNotNull();
        verify(paymentTransactionRepository).save(payment);
    }

    @Test
    void customerCannotDownloadAnotherCustomersInvoice() {
        when(paymentTransactionRepository.findById(paymentId))
                .thenReturn(Optional.of(payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY)));

        assertThatThrownBy(() -> invoiceService.downloadCustomerInvoice(paymentId, UUID.randomUUID()))
                .isInstanceOf(PaymentOperationFailedException.class)
                .hasMessage("You cannot download this invoice.");
    }

    @Test
    void adminCanDownloadInvoice() {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PAID);
        CustomerOrder order = order();
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.findById(targetId)).thenReturn(Optional.of(order));
        when(invoicePdfService.generateProductOrderInvoice(payment, customer, order)).thenReturn("%PDF".getBytes());

        InvoiceDownloadResult result = invoiceService.downloadAdminInvoice(paymentId);

        assertThat(result.type()).isEqualTo(InvoiceDownloadResult.Type.PDF);
    }

    @Test
    void cancelledPaymentHasNoInvoice() {
        when(paymentTransactionRepository.findById(paymentId))
                .thenReturn(Optional.of(payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.CANCELLED)));

        assertThatThrownBy(() -> invoiceService.downloadCustomerInvoice(paymentId, customerId))
                .isInstanceOf(PaymentOperationFailedException.class)
                .hasMessage("Invoice is not available for cancelled payments.");
    }

    @Test
    void pendingCashCancelledOrderHasNoInvoice() {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);
        CustomerOrder order = order();
        order.setStatus(OrderStatus.CANCELLED);
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerOrderRepository.findById(targetId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> invoiceService.downloadCustomerInvoice(paymentId, customerId))
                .isInstanceOf(PaymentOperationFailedException.class)
                .hasMessage("Invoice is not available for cancelled payments.");
    }

    @Test
    void pendingStripePaymentHasNoInvoice() {
        when(paymentTransactionRepository.findById(paymentId))
                .thenReturn(Optional.of(payment(PaymentMethod.STRIPE_CHECKOUT, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> invoiceService.downloadCustomerInvoice(paymentId, customerId))
                .isInstanceOf(PaymentOperationFailedException.class)
                .hasMessage("Invoice is not available until the payment is confirmed.");
    }

    @Test
    void paidStripePaymentWithPdfUrlReturnsRedirect() {
        PaymentTransaction payment = payment(PaymentMethod.STRIPE_CHECKOUT, PaymentStatus.PAID);
        payment.setStripeInvoicePdfUrl("https://stripe.test/invoice.pdf");
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        InvoiceDownloadResult result = invoiceService.downloadCustomerInvoice(paymentId, customerId);

        assertThat(result.type()).isEqualTo(InvoiceDownloadResult.Type.REDIRECT);
        assertThat(result.redirectUrl()).isEqualTo("https://stripe.test/invoice.pdf");
    }

    @Test
    void cashCustomPrintPaymentGeneratesPdfBytes() {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);
        payment.setTargetType(PaymentTargetType.CUSTOM_PRINT_REQUEST);
        var request = CustomPrintClientTestData.details(targetId, customerId, CustomPrintRequestStatus.ACCEPTED);
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customPrintRequestService.getRequestDetailsForAdmin(targetId))
                .thenReturn(request);
        when(invoicePdfService.generateCustomPrintInvoice(
                payment,
                customer,
                request
        )).thenReturn("%PDF".getBytes());

        InvoiceDownloadResult result = invoiceService.downloadCustomerInvoice(paymentId, customerId);

        assertThat(result.pdfBytes()).containsExactly("%PDF".getBytes());
    }

    private PaymentTransaction payment(PaymentMethod method, PaymentStatus status) {
        return PaymentTransaction.builder()
                .id(paymentId)
                .customerId(customerId)
                .targetType(PaymentTargetType.PRODUCT_ORDER)
                .targetId(targetId)
                .amount(new BigDecimal("12.50"))
                .currency("eur")
                .paymentMethod(method)
                .paymentStatus(status)
                .build();
    }

    private CustomerOrder order() {
        Product product = ProductTestData.product("Invoice Product");
        product.setId(UUID.randomUUID());
        return OrderTestData.order(customer, product, OrderStatus.PENDING);
    }
}
