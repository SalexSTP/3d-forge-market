package com.aleksandar.threedforgemarket.exception.auth;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("The requested user was not found.");
    }
}
