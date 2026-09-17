package com.eneik.epidemiology.auth;

/**
 * Base domain exception representing OIDC ID token validation failures.
 */
public abstract class OidcValidationException extends RuntimeException {

    private final String errorCode;

    public OidcValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OidcValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class OidcMissingClaimException extends OidcValidationException {
        private final String claimName;

        public OidcMissingClaimException(String claimName) {
            super("OIDC_MISSING_CLAIM", "OIDC token is missing required claim: " + claimName);
            this.claimName = claimName;
        }

        public String getClaimName() {
            return claimName;
        }
    }

    public static class OidcInvalidSignatureException extends OidcValidationException {
        public OidcInvalidSignatureException(String message) {
            super("OIDC_INVALID_SIGNATURE", message);
        }

        public OidcInvalidSignatureException(String message, Throwable cause) {
            super("OIDC_INVALID_SIGNATURE", message, cause);
        }
    }

    public static class OidcInvalidIssuerException extends OidcValidationException {
        private final String actualIssuer;
        private final String expectedIssuer;

        public OidcInvalidIssuerException(String actualIssuer, String expectedIssuer) {
            super("OIDC_INVALID_ISSUER", String.format("OIDC token issuer '%s' does not match expected issuer '%s'", actualIssuer, expectedIssuer));
            this.actualIssuer = actualIssuer;
            this.expectedIssuer = expectedIssuer;
        }

        public String getActualIssuer() {
            return actualIssuer;
        }

        public String getExpectedIssuer() {
            return expectedIssuer;
        }
    }

    public static class OidcInvalidAudienceException extends OidcValidationException {
        private final String actualAudience;
        private final String expectedAudience;

        public OidcInvalidAudienceException(String actualAudience, String expectedAudience) {
            super("OIDC_INVALID_AUDIENCE", String.format("OIDC token audience '%s' does not match expected audience '%s'", actualAudience, expectedAudience));
            this.actualAudience = actualAudience;
            this.expectedAudience = expectedAudience;
        }

        public String getActualAudience() {
            return actualAudience;
        }

        public String getExpectedAudience() {
            return expectedAudience;
        }
    }

    public static class OidcTokenExpiredException extends OidcValidationException {
        public OidcTokenExpiredException(String message) {
            super("OIDC_TOKEN_EXPIRED", message);
        }
    }

    public static class OidcMalformedTokenException extends OidcValidationException {
        public OidcMalformedTokenException(String message) {
            super("OIDC_MALFORMED_TOKEN", message);
        }

        public OidcMalformedTokenException(String message, Throwable cause) {
            super("OIDC_MALFORMED_TOKEN", message, cause);
        }
    }
}
