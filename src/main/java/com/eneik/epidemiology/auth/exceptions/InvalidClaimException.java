package com.eneik.epidemiology.auth.exceptions;

public class InvalidClaimException extends OidcValidationException {
    public InvalidClaimException(String message) {
        super(message);
    }
}
