package com.eneik.epidemiology.auth;

public class LmsServerException extends RuntimeException {
    public LmsServerException(String message) {
        super(message);
    }

    public LmsServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
