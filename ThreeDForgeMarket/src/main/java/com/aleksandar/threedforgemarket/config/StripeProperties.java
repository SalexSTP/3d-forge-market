package com.aleksandar.threedforgemarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        boolean enabled,
        String secretKey,
        String webhookSecret,
        String checkoutDisplayName
) {

    public StripeProperties {
        if (checkoutDisplayName == null || checkoutDisplayName.isBlank()) {
            checkoutDisplayName = "3DForgeMarket";
        }
    }

    public boolean isCheckoutConfigured() {
        return enabled && hasText(secretKey);
    }

    public boolean isWebhookConfigured() {
        return enabled && hasText(secretKey) && hasText(webhookSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
