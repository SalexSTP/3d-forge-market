package com.aleksandar.threedforgemarket.exception.customprint;

public class CustomPrintServiceUnavailableException extends RuntimeException {
    public CustomPrintServiceUnavailableException() {
        super("Custom print requests are temporarily unavailable. Please try again later.");
    }
}
