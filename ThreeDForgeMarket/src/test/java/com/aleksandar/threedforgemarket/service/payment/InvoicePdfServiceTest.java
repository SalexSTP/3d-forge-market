package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.testdata.OrderTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfServiceTest {

    private final InvoicePdfService invoicePdfService = new InvoicePdfService();

    @Test
    void generatedProductOrderInvoiceContainsProfessionalInvoiceContent() throws Exception {
        UUID paymentId = UUID.fromString("15e4b42e-e465-422b-9fc2-8218e3e082e0");
        User customer = UserTestData.customer("invoice_customer");
        customer.setId(UUID.randomUUID());
        customer.setEmail("invoice.customer@example.com");
        Product product = ProductTestData.product("Modern Simple Hook");
        product.setId(UUID.randomUUID());
        CustomerOrder order = OrderTestData.order(customer, product, OrderStatus.PENDING);
        order.setQuantity(2);
        order.setTotalPrice(new BigDecimal("24.00"));
        PaymentTransaction payment = PaymentTransaction.builder()
                .id(paymentId)
                .customerId(customer.getId())
                .targetType(PaymentTargetType.PRODUCT_ORDER)
                .targetId(UUID.randomUUID())
                .amount(new BigDecimal("24.00"))
                .currency("eur")
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(PaymentStatus.PENDING_CASH_ON_DELIVERY)
                .createdOn(LocalDateTime.of(2026, 8, 9, 12, 0))
                .build();

        byte[] pdfBytes = invoicePdfService.generateProductOrderInvoice(payment, customer, order);

        String text;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            text = new PDFTextStripper().getText(document);
        }

        assertThat(text)
                .contains("Invoice")
                .contains("3DForgeMarket")
                .contains("INV-15E4B42E")
                .contains("invoice.customer@example.com")
                .contains("€24.00")
                .contains("Modern Simple Hook")
                .doesNotContain("Payment details")
                .doesNotContain("Payment method:")
                .doesNotContain("Payment status:");
    }
}
