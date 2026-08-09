package com.aleksandar.threedforgemarket.model.dto.payment;

public record PaymentStartResult(String redirectUrl, boolean externalRedirect) {

    public static PaymentStartResult local(String redirectUrl) {
        return new PaymentStartResult(redirectUrl, false);
    }

    public static PaymentStartResult external(String redirectUrl) {
        return new PaymentStartResult(redirectUrl, true);
    }
}
