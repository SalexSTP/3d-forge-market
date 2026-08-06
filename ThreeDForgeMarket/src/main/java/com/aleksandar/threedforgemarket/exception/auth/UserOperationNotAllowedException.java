package com.aleksandar.threedforgemarket.exception.auth;

public class UserOperationNotAllowedException extends RuntimeException {
    public UserOperationNotAllowedException(String message) {
        super(message);
    }
}
