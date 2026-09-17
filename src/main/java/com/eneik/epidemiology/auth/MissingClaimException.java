package com.eneik.epidemiology.auth;

public class MissingClaimException extends RuntimeException {
    public MissingClaimException(String message) {
        super(message);
    }

    public MissingClaimException(String message, Throwable cause) {
        super(message, cause);
    }
}
