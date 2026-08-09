package com.aleksandar.threedforgemarket.exception.customprint;

public class CustomPrintRequestNotFoundException extends RuntimeException {
    public CustomPrintRequestNotFoundException() {
        super("Custom print request was not found.");
    }
}
