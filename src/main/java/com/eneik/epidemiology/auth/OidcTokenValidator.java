package com.eneik.epidemiology.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Service component performing strict, explicit OIDC token verification
 * and returning typed constructive proof objects (Result/Exceptions).
 */
public class OidcTokenValidator {
    private static final Logger log = LoggerFactory.getLogger(OidcTokenValidator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public OidcTokenValidationResult validateOidcToken(
            String token,
            JwkProvider jwkProvider,
            String expectedServerUrl,
            String expectedClientId
    ) {
        if (token == null || token.trim().isEmpty()) {
            return OidcTokenValidationResult.failure(
                    new OidcValidationException.OidcMalformedTokenException("OIDC token must not be null or empty")
            );
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return OidcTokenValidationResult.failure(
                    new OidcValidationException.OidcMalformedTokenException("OIDC token format invalid: must contain header, payload, and signature")
            );
        }

        // 1. Verify Cryptographic Signature and Header
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode headerNode = MAPPER.readTree(headerJson);
            if (!headerNode.has("kid") || headerNode.get("kid").isNull()) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcInvalidSignatureException("OIDC token header missing 'kid' claim")
                );
            }
            String keyId = headerNode.get("kid").asText();
            Jwk jwk = jwkProvider.get(keyId);
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);

            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().before(new java.util.Date())) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcTokenExpiredException("OIDC token has expired at " + jwt.getExpiresAt())
                );
            }
        } catch (TokenExpiredException e) {
            log.warn("OIDC token expired: {}", e.getMessage());
            return OidcTokenValidationResult.failure(
                    new OidcValidationException.OidcTokenExpiredException("OIDC token has expired: " + e.getMessage())
            );
        } catch (com.auth0.jwk.JwkException | com.auth0.jwt.exceptions.JWTVerificationException e) {
            log.warn("OIDC token signature or verification error: {}", e.getMessage());
            return OidcTokenValidationResult.failure(
                    new OidcValidationException.OidcInvalidSignatureException("OIDC token signature validation failed: " + e.getMessage(), e)
            );
        } catch (Exception e) {
            log.warn("OIDC token header parsing error: {}", e.getMessage());
            return OidcTokenValidationResult.failure(
                    new OidcValidationException.OidcMalformedTokenException("OIDC token header is malformed: " + e.getMessage(), e)
            );
        }

        // 2. Extract and Validate Payload Claims
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode claims = MAPPER.readTree(payloadJson);

            // Validate 'iss' (Issuer) claim
            if (!claims.has("iss") || claims.get("iss").isNull()) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcMissingClaimException("iss")
                );
            }
            String tokenIssuer = claims.get("iss").asText().trim();
            if (expectedServerUrl != null && !tokenIssuer.equalsIgnoreCase(expectedServerUrl.trim())) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcInvalidIssuerException(tokenIssuer, expectedServerUrl.trim())
                );
            }

            // Validate 'aud' (Audience) claim
            if (!claims.has("aud") || claims.get("aud").isNull()) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcMissingClaimException("aud")
                );
            }
            boolean audienceValid = false;
            JsonNode audNode = claims.get("aud");
            String actualAudience = audNode.toString();
            if (audNode.isArray()) {
                for (JsonNode element : audNode) {
                    if (expectedClientId != null && expectedClientId.trim().equalsIgnoreCase(element.asText().trim())) {
                        audienceValid = true;
                        break;
                    }
                }
            } else {
                actualAudience = audNode.asText();
                if (expectedClientId != null && expectedClientId.trim().equalsIgnoreCase(actualAudience.trim())) {
                    audienceValid = true;
                }
            }
            if (!audienceValid) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcInvalidAudienceException(actualAudience, expectedClientId)
                );
            }

            // Extract User Identity and Profile Claims
            String username = claims.has("username") ? claims.get("username").asText() :
                    (claims.has("preferred_username") ? claims.get("preferred_username").asText() :
                            (claims.has("sub") ? claims.get("sub").asText() : null));

            if (username == null || username.trim().isEmpty()) {
                return OidcTokenValidationResult.failure(
                        new OidcValidationException.OidcMissingClaimException("username/sub")
                );
            }

            String moodleRole = claims.has("moodle_role") ? claims.get("moodle_role").asText() :
                    (claims.has("role") ? claims.get("role").asText() : "Пользователь");

            String department = claims.has("department") ? claims.get("department").asText() :
                    (claims.has("custom_department") ? claims.get("custom_department").asText() : "");
            String email = claims.has("email") ? claims.get("email").asText() : "";
            String fullName = claims.has("full_name") ? claims.get("full_name").asText() :
                    (claims.has("name") ? claims.get("name").asText() : username);
            boolean suspended = claims.has("suspended") && claims.get("suspended").asBoolean();
            if (!suspended && claims.has("deleted")) {
                suspended = claims.get("deleted").asBoolean();
            }
            String courses = claims.has("courses") ? claims.get("courses").asText() :
                    (claims.has("custom_courses") ? claims.get("custom_courses").asText() : "");

            return OidcTokenValidationResult.success(username, moodleRole, department, email, fullName, courses, suspended);
        } catch (Exception e) {
            log.error("Error extracting payload claims from OIDC token", e);
            return OidcTokenValidationResult.failure(
                    new OidcValidationException.OidcMalformedTokenException("OIDC token payload is malformed: " + e.getMessage(), e)
            );
        }
    }
}
