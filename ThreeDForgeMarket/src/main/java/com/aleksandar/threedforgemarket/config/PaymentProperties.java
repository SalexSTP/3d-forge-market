package com.aleksandar.threedforgemarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(String currency) {

    public String currency() {
        return currency == null || currency.isBlank() ? "eur" : currency.toLowerCase();
    }
}
