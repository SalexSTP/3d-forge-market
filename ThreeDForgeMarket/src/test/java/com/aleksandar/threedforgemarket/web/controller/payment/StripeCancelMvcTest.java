package com.aleksandar.threedforgemarket.web.controller.payment;

import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.payment.PaymentTransactionRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.service.payment.StripeCheckoutClient;
import com.aleksandar.threedforgemarket.service.payment.StripeCheckoutResult;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "stripe.enabled=true",
        "stripe.secret-key=sk_test_key",
        "stripe.webhook-secret=whsec_test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StripeCancelMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StripeCheckoutClient stripeCheckoutClient;

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
    private User admin;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        reviewRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("stripe_cancel_customer"));
        admin = userRepository.save(UserTestData.admin("stripe_cancel_admin"));
        product = productRepository.save(ProductTestData.product("Stripe Cancel Product"));

        when(stripeCheckoutClient.createCheckoutSession(any()))
                .thenReturn(new StripeCheckoutResult("cs_test_cancel", "https://checkout.stripe.test/cancel"));
    }

    @Test
    void customerStartsStripeOrderPaymentCancelsAndReturnsToPrefilledOrderForm() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("quantity", "3")
                        .param("paymentMethod", "STRIPE_CHECKOUT")
                        .param("deliveryAddress", "123 Stripe Street")
                        .param("customerNote", "Leave near reception"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.test/cancel"));

        PaymentTransaction paymentTransaction = paymentTransactionRepository.findAll().get(0);

        MvcResult cancelResult = mockMvc.perform(get("/payments/stripe/cancel")
                        .with(customer(customer.getId()))
                        .param("paymentTransactionId", paymentTransaction.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/create?productId=" + product.getId()))
                .andExpect(flash().attributeExists("orderForm"))
                .andExpect(flash().attribute("infoMessage", "Online payment was cancelled. You can review your order and try again."))
                .andReturn();

        PaymentTransaction cancelledPayment = paymentTransactionRepository.findById(paymentTransaction.getId()).orElseThrow();
        assertThat(cancelledPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(cancelledPayment.getCancelledOn()).isNotNull();

        CustomerOrder hiddenOrder = customerOrderRepository.findAll().get(0);
        assertThat(hiddenOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(hiddenOrder.isDeletedFromCustomerHistory()).isTrue();
        assertThat(hiddenOrder.isDeletedFromAdminHistory()).isTrue();

        mockMvc.perform(get("/orders/create")
                        .with(customer(customer.getId()))
                        .param("productId", product.getId().toString())
                        .flashAttrs(cancelResult.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(view().name("order/create"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("123 Stripe Street")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("Leave near reception")));
    }

    @Test
    void productStripeSuccessRedirectsToOrdersWithoutMarkingPaymentPaid() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("quantity", "2")
                        .param("paymentMethod", "STRIPE_CHECKOUT")
                        .param("deliveryAddress", "123 Stripe Street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.test/cancel"));

        PaymentTransaction paymentTransaction = paymentTransactionRepository.findAll().get(0);

        mockMvc.perform(get("/payments/stripe/success")
                        .with(customer(customer.getId()))
                        .param("session_id", paymentTransaction.getStripeCheckoutSessionId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Payment completed. Your order is now waiting for admin review."
                ));

        PaymentTransaction unchangedPayment = paymentTransactionRepository.findById(paymentTransaction.getId()).orElseThrow();
        assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(unchangedPayment.getPaidOn()).isNull();
    }

    @Test
    void cancelledStripeOrderIsHiddenFromCustomerAndAdminOrderLists() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("productId", product.getId().toString())
                        .param("quantity", "1")
                        .param("paymentMethod", "STRIPE_CHECKOUT")
                        .param("deliveryAddress", "123 Stripe Street"))
                .andExpect(status().is3xxRedirection());

        PaymentTransaction paymentTransaction = paymentTransactionRepository.findAll().get(0);

        mockMvc.perform(get("/payments/stripe/cancel")
                        .with(customer(customer.getId()))
                        .param("paymentTransactionId", paymentTransaction.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/orders/my").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(not(containsString("Stripe Cancel Product"))));

        mockMvc.perform(get("/admin/orders").with(admin(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(not(containsString("Stripe Cancel Product"))));
    }
}
