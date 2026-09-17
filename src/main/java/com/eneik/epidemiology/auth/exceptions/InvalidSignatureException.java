package com.eneik.epidemiology.auth.exceptions;

public class InvalidSignatureException extends OidcValidationException {
    public InvalidSignatureException(String message) {
        super(message);
    }
    public InvalidSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
