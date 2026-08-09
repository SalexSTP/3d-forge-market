package com.aleksandar.threedforgemarket.web.controller.payment;

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
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.OrderTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User customer;
    private User otherCustomer;
    private User admin;
    private Product product;
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("invoice_mvc_customer"));
        otherCustomer = userRepository.save(UserTestData.customer("invoice_mvc_other"));
        admin = userRepository.save(UserTestData.admin("invoice_mvc_admin"));
        product = productRepository.save(ProductTestData.product("Invoice MVC Product"));
        order = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));
    }

    @Test
    void customerInvoiceEndpointRequiresAuthentication() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

        mockMvc.perform(get("/payments/{id}/invoice", payment.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
    }

    @Test
    void customerInvoiceEndpointRejectsAnotherUsersPayment() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

        mockMvc.perform(get("/payments/{id}/invoice", payment.getId()).with(customer(otherCustomer.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/my"));
    }

    @Test
    void adminInvoiceEndpointRequiresAdmin() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

        mockMvc.perform(get("/admin/payments/{id}/invoice", payment.getId()).with(customer(customer.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashInvoiceEndpointReturnsApplicationPdf() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

        mockMvc.perform(get("/payments/{id}/invoice", payment.getId()).with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("invoice-" + payment.getId() + ".pdf")));
    }

    @Test
    void stripeInvoiceEndpointRedirectsToStoredPdfUrl() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.STRIPE_CHECKOUT, PaymentStatus.PAID);
        payment.setStripeInvoicePdfUrl("https://stripe.test/invoice.pdf");
        paymentTransactionRepository.save(payment);

        mockMvc.perform(get("/payments/{id}/invoice", payment.getId()).with(customer(customer.getId())))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://stripe.test/invoice.pdf"));
    }

    @Test
    void invoiceButtonAppearsOnlyWhenInvoiceIsAvailable() throws Exception {
        PaymentTransaction availablePayment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);
        CustomerOrder pendingStripeOrder = customerOrderRepository.save(OrderTestData.order(customer, product, OrderStatus.PENDING));
        PaymentTransaction unavailablePayment = paymentTransactionRepository.save(PaymentTransaction.builder()
                .customerId(customer.getId())
                .targetType(PaymentTargetType.PRODUCT_ORDER)
                .targetId(pendingStripeOrder.getId())
                .amount(new BigDecimal("12.50"))
                .currency("eur")
                .paymentMethod(PaymentMethod.STRIPE_CHECKOUT)
                .paymentStatus(PaymentStatus.PENDING)
                .build());

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invoice")))
                .andExpect(content().string(not(containsString("Download invoice"))))
                .andExpect(content().string(containsString("/payments/" + availablePayment.getId() + "/invoice")))
                .andExpect(content().string(not(containsString("/payments/" + unavailablePayment.getId() + "/invoice"))));
    }

    @Test
    void customerCancelledPendingUnpaidCashOrderDoesNotShowInvoiceButton() throws Exception {
        order.setStatus(OrderStatus.CANCELLED);
        customerOrderRepository.save(order);
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/payments/" + payment.getId() + "/invoice"))));
    }

    @Test
    void cashPaidCancelledOrderKeepsInvoiceButton() throws Exception {
        order.setStatus(OrderStatus.CANCELLED);
        customerOrderRepository.save(order);
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PAID);

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/payments/" + payment.getId() + "/invoice")));
    }

    @Test
    void cancelledUnpaidStripePaymentDoesNotShowInvoiceButton() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.STRIPE_CHECKOUT, PaymentStatus.CANCELLED);

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/payments/" + payment.getId() + "/invoice"))));
    }

    @Test
    void paidStripePaymentWithInvoiceUrlShowsInvoiceButton() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.STRIPE_CHECKOUT, PaymentStatus.PAID);
        payment.setStripeInvoicePdfUrl("https://stripe.test/invoice.pdf");
        paymentTransactionRepository.save(payment);

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invoice")))
                .andExpect(content().string(containsString("/payments/" + payment.getId() + "/invoice")));
    }

    @Test
    void adminCanAccessInvoiceAction() throws Exception {
        PaymentTransaction payment = payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY);

        mockMvc.perform(get("/admin/orders").with(admin(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/payments/" + payment.getId() + "/invoice")));
    }

    private PaymentTransaction payment(PaymentMethod method, PaymentStatus status) {
        return paymentTransactionRepository.save(PaymentTransaction.builder()
                .customerId(customer.getId())
                .targetType(PaymentTargetType.PRODUCT_ORDER)
                .targetId(order.getId())
                .amount(new BigDecimal("12.50"))
                .currency("eur")
                .paymentMethod(method)
                .paymentStatus(status)
                .build());
    }
}
