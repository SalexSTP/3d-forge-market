package com.aleksandar.customprintservice.exception;

public class CustomPrintRequestOperationNotAllowedException extends RuntimeException {

    public CustomPrintRequestOperationNotAllowedException(String message) {
        super(message);
    }
}
