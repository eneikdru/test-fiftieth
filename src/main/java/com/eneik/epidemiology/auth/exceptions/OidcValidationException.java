package com.eneik.epidemiology.auth.exceptions;

public class OidcValidationException extends com.eneik.epidemiology.auth.OidcValidationException {
    public OidcValidationException(String message) {
        super("OIDC_VALIDATION_ERROR", message);
    }

    public OidcValidationException(String message, Throwable cause) {
        super("OIDC_VALIDATION_ERROR", message, cause);
    }
}
