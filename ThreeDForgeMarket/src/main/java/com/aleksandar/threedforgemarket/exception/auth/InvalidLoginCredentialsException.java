package com.aleksandar.threedforgemarket.exception.auth;

public class InvalidLoginCredentialsException extends RuntimeException {
    public InvalidLoginCredentialsException() {
        super("Invalid username, email, or password.");
    }
}
