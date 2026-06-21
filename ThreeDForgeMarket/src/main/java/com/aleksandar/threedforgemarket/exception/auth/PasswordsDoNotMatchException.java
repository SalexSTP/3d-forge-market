package com.aleksandar.threedforgemarket.exception.auth;

public class PasswordsDoNotMatchException extends RuntimeException {
    public PasswordsDoNotMatchException() {
        super("Passwords do not match.");
    }
}
