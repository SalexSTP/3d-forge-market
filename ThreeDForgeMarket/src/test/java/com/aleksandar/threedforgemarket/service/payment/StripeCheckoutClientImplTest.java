package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.config.StripeProperties;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StripeCheckoutClientImplTest {

    @Test
    void checkoutSessionParamsUseEurCardOnlyEnglishLocaleAndPublicProductImage() {
        StripeCheckoutClientImpl client = new StripeCheckoutClientImpl(
                new StripeProperties(true, "sk_test_key", "whsec_test", "3DForgeMarket")
        );

        SessionCreateParams params = client.buildCreateParams(request("https://example.com/product.jpg"));

        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(params.getLocale()).isEqualTo(SessionCreateParams.Locale.EN);
        assertThat(params.getPaymentMethodTypes())
                .containsExactly(SessionCreateParams.PaymentMethodType.CARD);
        assertThat(params.getBrandingSettings().getDisplayName()).isEqualTo("3DForgeMarket");
        assertThat(params.getMetadata()).containsEntry("targetType", PaymentTargetType.PRODUCT_ORDER.name());
        assertThat(params.getInvoiceCreation().getEnabled()).isTrue();
        assertThat(params.getInvoiceCreation().getInvoiceData().getDescription()).isEqualTo("Modern Simple Hook");
        assertThat(params.getInvoiceCreation().getInvoiceData().getMetadata())
                .containsEntry("targetType", PaymentTargetType.PRODUCT_ORDER.name())
                .containsKeys("paymentTransactionId", "targetId");

        SessionCreateParams.LineItem lineItem = params.getLineItems().get(0);
        assertThat(lineItem.getPriceData().getCurrency()).isEqualTo("eur");
        assertThat(lineItem.getPriceData().getUnitAmount()).isEqualTo(4250L);
        assertThat(lineItem.getPriceData().getProductData().getName()).isEqualTo("Modern Simple Hook");
        assertThat(lineItem.getPriceData().getProductData().getImages())
                .containsExactly("https://example.com/product.jpg");
    }

    @Test
    void checkoutSessionParamsOmitProductImageWhenMissing() {
        StripeCheckoutClientImpl client = new StripeCheckoutClientImpl(
                new StripeProperties(true, "sk_test_key", "whsec_test", "3DForgeMarket")
        );

        SessionCreateParams params = client.buildCreateParams(request(null));

        assertThat(params.getLineItems().get(0).getPriceData().getProductData().getImages())
                .isNull();
    }

    private StripeCheckoutRequest request(String imageUrl) {
        return new StripeCheckoutRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentTargetType.PRODUCT_ORDER,
                UUID.randomUUID(),
                new BigDecimal("42.50"),
                "eur",
                "Modern Simple Hook",
                imageUrl,
                "http://localhost/payments/stripe/success?session_id={CHECKOUT_SESSION_ID}",
                "http://localhost/payments/stripe/cancel?paymentTransactionId=123"
        );
    }
}
