package com.eneik.epidemiology.exception;

public class PrivacyException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;

    public PrivacyException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
