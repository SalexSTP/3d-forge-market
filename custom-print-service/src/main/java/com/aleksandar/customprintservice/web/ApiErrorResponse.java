package com.aleksandar.customprintservice.web;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        LocalDateTime timestamp
) {

    public static ApiErrorResponse of(int status, String message) {
        return new ApiErrorResponse(status, message, Map.of(), LocalDateTime.now());
    }

    public static ApiErrorResponse of(int status, String message, Map<String, String> errors) {
        return new ApiErrorResponse(status, message, errors, LocalDateTime.now());
    }
}
