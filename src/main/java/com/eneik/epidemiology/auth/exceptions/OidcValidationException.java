package com.eneik.epidemiology.auth.exceptions;

public class OidcValidationException extends RuntimeException {
    public OidcValidationException(String message) {
        super(message);
    }

    public OidcValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
