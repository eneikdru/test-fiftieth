package com.eneik.epidemiology.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OidcTokenValidationResultTest {

    private KeyPair rsaKeyPair;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;
    private JwkProvider jwkProvider;
    private OidcTokenValidator validator;

    private static final String TRUSTED_ISSUER = "https://moodle.epidemiology-inst.ru";
    private static final String CLIENT_ID = "epidemiology_portal";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        rsaKeyPair = kpg.generateKeyPair();
        rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();

        jwkProvider = new JwkProvider() {
            @Override
            public Jwk get(String keyId) throws JwkException {
                if ("test-kid".equals(keyId)) {
                    return Jwk.fromValues(Map.of(
                            "kid", "test-kid",
                            "kty", "RSA",
                            "alg", "RS256",
                            "use", "sig",
                            "n", Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getModulus().toByteArray()),
                            "e", Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getPublicExponent().toByteArray())
                    ));
                }
                throw new JwkException("Key not found: " + keyId);
            }
        };

        validator = new OidcTokenValidator();
    }

    private String createToken(String username, String role, String department, String courses, String issuer, String audience, Date expiresAt) {
        try {
            Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, rsaPrivateKey);
            com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                    .withKeyId("test-kid")
                    .withSubject(username)
                    .withClaim("username", username)
                    .withClaim("role", role)
                    .withIssuedAt(new Date());

            if (expiresAt != null) builder.withExpiresAt(expiresAt);
            if (department != null) builder.withClaim("department", department);
            if (courses != null) builder.withClaim("courses", courses);
            if (issuer != null) builder.withIssuer(issuer);
            if (audience != null) builder.withAudience(audience);

            return builder.sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Given a valid OIDC token, When validated, Then returns Success result with profile data")
    void testValidateOidcToken_Success() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String token = createToken("valid_user", "Исследователь", "Геномика", "EPID-101", TRUSTED_ISSUER, CLIENT_ID, futureExpiry);

        OidcTokenValidationResult result = validator.validateOidcToken(token, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertTrue(result.profile().isPresent());
        assertTrue(result.failure().isEmpty());

        OidcTokenValidationResult.OidcProfile profile = result.profile().get();
        assertEquals("valid_user", profile.username());
        assertEquals("Исследователь", profile.moodleRole());
        assertEquals("Геномика", profile.department());
        assertEquals("EPID-101", profile.courses());
    }

    @Test
    @DisplayName("Given an OIDC token missing 'iss' claim, When validated, Then returns Failure with OidcMissingClaimException")
    void testValidateOidcToken_MissingIssuer_ReturnsMissingClaimException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String token = createToken("user_no_iss", "USER", null, null, null, CLIENT_ID, futureExpiry);

        OidcTokenValidationResult result = validator.validateOidcToken(token, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());
        assertTrue(result.failure().isPresent());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcMissingClaimException.class, ex);
        OidcValidationException.OidcMissingClaimException missingClaimEx = (OidcValidationException.OidcMissingClaimException) ex;
        assertEquals("iss", missingClaimEx.getClaimName());
        assertEquals("OIDC_MISSING_CLAIM", missingClaimEx.getErrorCode());
    }

    @Test
    @DisplayName("Given an OIDC token missing 'aud' claim, When validated, Then returns Failure with OidcMissingClaimException")
    void testValidateOidcToken_MissingAudience_ReturnsMissingClaimException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String token = createToken("user_no_aud", "USER", null, null, TRUSTED_ISSUER, null, futureExpiry);

        OidcTokenValidationResult result = validator.validateOidcToken(token, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());
        assertTrue(result.failure().isPresent());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcMissingClaimException.class, ex);
        OidcValidationException.OidcMissingClaimException missingClaimEx = (OidcValidationException.OidcMissingClaimException) ex;
        assertEquals("aud", missingClaimEx.getClaimName());
        assertEquals("OIDC_MISSING_CLAIM", missingClaimEx.getErrorCode());
    }

    @Test
    @DisplayName("Given an OIDC token with mismatched issuer, When validated, Then returns Failure with OidcInvalidIssuerException")
    void testValidateOidcToken_InvalidIssuer_ReturnsInvalidIssuerException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String token = createToken("user_bad_iss", "USER", null, null, "https://untrusted-issuer.org", CLIENT_ID, futureExpiry);

        OidcTokenValidationResult result = validator.validateOidcToken(token, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcInvalidIssuerException.class, ex);
        OidcValidationException.OidcInvalidIssuerException issEx = (OidcValidationException.OidcInvalidIssuerException) ex;
        assertEquals("https://untrusted-issuer.org", issEx.getActualIssuer());
        assertEquals(TRUSTED_ISSUER, issEx.getExpectedIssuer());
    }

    @Test
    @DisplayName("Given an OIDC token with mismatched audience, When validated, Then returns Failure with OidcInvalidAudienceException")
    void testValidateOidcToken_InvalidAudience_ReturnsInvalidAudienceException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String token = createToken("user_bad_aud", "USER", null, null, TRUSTED_ISSUER, "other_client_id", futureExpiry);

        OidcTokenValidationResult result = validator.validateOidcToken(token, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcInvalidAudienceException.class, ex);
        OidcValidationException.OidcInvalidAudienceException audEx = (OidcValidationException.OidcInvalidAudienceException) ex;
        assertEquals("other_client_id", audEx.getActualAudience());
        assertEquals(CLIENT_ID, audEx.getExpectedAudience());
    }

    @Test
    @DisplayName("Given an OIDC token with invalid signature, When validated, Then returns Failure with OidcInvalidSignatureException")
    void testValidateOidcToken_InvalidSignature_ReturnsInvalidSignatureException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String token = createToken("user_bad_sig", "USER", null, null, TRUSTED_ISSUER, CLIENT_ID, futureExpiry);
        String tamperedToken = token + "tampered_bytes";

        OidcTokenValidationResult result = validator.validateOidcToken(tamperedToken, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcInvalidSignatureException.class, ex);
        assertEquals("OIDC_INVALID_SIGNATURE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given an expired OIDC token, When validated, Then returns Failure with OidcTokenExpiredException")
    void testValidateOidcToken_ExpiredToken_ReturnsExpiredException() {
        Date pastExpiry = new Date(System.currentTimeMillis() - 3600000);
        String token = createToken("user_expired", "USER", null, null, TRUSTED_ISSUER, CLIENT_ID, pastExpiry);

        OidcTokenValidationResult result = validator.validateOidcToken(token, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcTokenExpiredException.class, ex);
        assertEquals("OIDC_TOKEN_EXPIRED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given a malformed token string, When validated, Then returns Failure with OidcMalformedTokenException")
    void testValidateOidcToken_MalformedToken_ReturnsMalformedTokenException() {
        String malformedToken = "invalid.jwt.payload.too.many.parts";

        OidcTokenValidationResult result = validator.validateOidcToken(malformedToken, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcMalformedTokenException.class, ex);
        assertEquals("OIDC_MALFORMED_TOKEN", ex.getErrorCode());
    }
}
