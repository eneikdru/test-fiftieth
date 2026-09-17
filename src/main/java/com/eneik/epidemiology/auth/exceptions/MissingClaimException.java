package com.eneik.epidemiology.auth.exceptions;

public class MissingClaimException extends OidcValidationException {
    public MissingClaimException(String message) {
        super(message);
    }
}
