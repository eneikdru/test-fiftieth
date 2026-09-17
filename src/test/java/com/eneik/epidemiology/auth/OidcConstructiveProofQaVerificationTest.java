package com.eneik.epidemiology.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.eneik.epidemiology.auth.exceptions.InvalidClaimException;
import com.eneik.epidemiology.auth.exceptions.InvalidSignatureException;
import com.eneik.epidemiology.auth.exceptions.MissingClaimException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OidcConstructiveProofQaVerificationTest {

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
                if ("valid-kid".equals(keyId)) {
                    return Jwk.fromValues(Map.of(
                            "kid", "valid-kid",
                            "kty", "RSA",
                            "alg", "RS256",
                            "use", "sig",
                            "n", Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getModulus().toByteArray()),
                            "e", Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getPublicExponent().toByteArray())
                    ));
                }
                throw new JwkException("Key ID not found: " + keyId);
            }
        };

        validator = new OidcTokenValidator();
    }

    private String createToken(String username, String role, String issuer, String audience, Date expiresAt, String kid) {
        try {
            Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, rsaPrivateKey);
            com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                    .withKeyId(kid != null ? kid : "valid-kid")
                    .withIssuedAt(new Date());

            if (username != null) {
                builder.withSubject(username).withClaim("username", username);
            }
            if (role != null) {
                builder.withClaim("role", role);
            }
            if (expiresAt != null) {
                builder.withExpiresAt(expiresAt);
            }
            if (issuer != null) {
                builder.withIssuer(issuer);
            }
            if (audience != null) {
                builder.withAudience(audience);
            }

            return builder.sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Given simulated OIDC token missing 'iss' claim, When validated, Then specific MissingClaimException is returned and preserved")
    void testOidcToken_MissingIssuerClaim_PreservesMissingClaimException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String tokenWithoutIss = createToken("epidem_user", "RESEARCHER", null, CLIENT_ID, futureExpiry, "valid-kid");

        OidcTokenValidationResult result = validator.validateOidcToken(tokenWithoutIss, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure(), "Expected validation failure for missing 'iss' claim");
        assertTrue(result.failure().isPresent(), "Failure object should contain constructive proof exception");

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcMissingClaimException.class, ex,
                "Expected specific OidcMissingClaimException constructive proof");

        OidcValidationException.OidcMissingClaimException missingClaimEx = (OidcValidationException.OidcMissingClaimException) ex;
        assertEquals("iss", missingClaimEx.getClaimName());
        assertEquals("OIDC_MISSING_CLAIM", missingClaimEx.getErrorCode());
        assertTrue(missingClaimEx.getMessage().contains("iss"), "Exception message must specify missing claim 'iss'");
    }

    @Test
    @DisplayName("Given simulated OIDC token missing 'aud' claim, When validated, Then specific MissingClaimException is returned and preserved")
    void testOidcToken_MissingAudienceClaim_PreservesMissingClaimException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String tokenWithoutAud = createToken("epidem_user", "RESEARCHER", TRUSTED_ISSUER, null, futureExpiry, "valid-kid");

        OidcTokenValidationResult result = validator.validateOidcToken(tokenWithoutAud, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure(), "Expected validation failure for missing 'aud' claim");
        assertTrue(result.failure().isPresent(), "Failure object should contain constructive proof exception");

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcMissingClaimException.class, ex,
                "Expected specific OidcMissingClaimException constructive proof");

        OidcValidationException.OidcMissingClaimException missingClaimEx = (OidcValidationException.OidcMissingClaimException) ex;
        assertEquals("aud", missingClaimEx.getClaimName());
        assertEquals("OIDC_MISSING_CLAIM", missingClaimEx.getErrorCode());
        assertTrue(missingClaimEx.getMessage().contains("aud"), "Exception message must specify missing claim 'aud'");
    }

    @Test
    @DisplayName("Given simulated OIDC token missing subject/username claim, When validated, Then specific MissingClaimException is returned")
    void testOidcToken_MissingUsernameClaim_PreservesMissingClaimException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String tokenWithoutUser = createToken(null, "RESEARCHER", TRUSTED_ISSUER, CLIENT_ID, futureExpiry, "valid-kid");

        OidcTokenValidationResult result = validator.validateOidcToken(tokenWithoutUser, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure(), "Expected validation failure for missing username/sub claim");
        assertTrue(result.failure().isPresent());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcMissingClaimException.class, ex);
        OidcValidationException.OidcMissingClaimException missingClaimEx = (OidcValidationException.OidcMissingClaimException) ex;
        assertEquals("username/sub", missingClaimEx.getClaimName());
    }

    @Test
    @DisplayName("Given simulated invalid OIDC signature, When validated, Then specific InvalidSignatureException is preserved")
    void testOidcToken_InvalidSignature_PreservesInvalidSignatureException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String validToken = createToken("epidem_user", "RESEARCHER", TRUSTED_ISSUER, CLIENT_ID, futureExpiry, "valid-kid");
        String tamperedSignatureToken = validToken + "tampered";

        OidcTokenValidationResult result = validator.validateOidcToken(tamperedSignatureToken, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure(), "Expected validation failure for invalid OIDC signature");
        assertTrue(result.failure().isPresent());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcInvalidSignatureException.class, ex,
                "Expected specific OidcInvalidSignatureException constructive proof");

        assertEquals("OIDC_INVALID_SIGNATURE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("signature"), "Exception message should indicate signature validation error");
    }

    @Test
    @DisplayName("Given simulated unknown key ID (kid) in OIDC header, When validated, Then InvalidSignatureException is preserved")
    void testOidcToken_UnknownKeyId_PreservesInvalidSignatureException() {
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String tokenWithUnknownKid = createToken("epidem_user", "RESEARCHER", TRUSTED_ISSUER, CLIENT_ID, futureExpiry, "unknown-kid");

        OidcTokenValidationResult result = validator.validateOidcToken(tokenWithUnknownKid, jwkProvider, TRUSTED_ISSUER, CLIENT_ID);

        assertTrue(result.isFailure());
        assertTrue(result.failure().isPresent());

        OidcValidationException ex = result.failure().get();
        assertInstanceOf(OidcValidationException.OidcInvalidSignatureException.class, ex);
        assertEquals("OIDC_INVALID_SIGNATURE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given AuthController.fetchOidcProfile executed with missing claims, Then throws specific MissingClaimException")
    void testAuthController_FetchOidcProfile_ThrowsMissingClaimException() throws Exception {
        AuthController authController = new AuthController(null, null, null, null, null, null);
        authController.setJwkProvider(jwkProvider);

        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String tokenWithoutIss = createToken("epidem_user", "RESEARCHER", null, CLIENT_ID, futureExpiry, "valid-kid");

        Method fetchOidcProfileMethod = AuthController.class.getDeclaredMethod("fetchOidcProfile", String.class);
        fetchOidcProfileMethod.setAccessible(true);

        try {
            fetchOidcProfileMethod.invoke(authController, tokenWithoutIss);
            fail("Expected ReflectionException wrapping MissingClaimException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertInstanceOf(MissingClaimException.class, cause,
                    "Expected MissingClaimException constructive proof when 'iss' claim is missing");
            assertTrue(cause.getMessage().contains("iss"));
        }
    }

    @Test
    @DisplayName("Given AuthController.fetchOidcProfile executed with invalid signature, Then throws specific InvalidSignatureException")
    void testAuthController_FetchOidcProfile_ThrowsInvalidSignatureException() throws Exception {
        AuthController authController = new AuthController(null, null, null, null, null, null);
        authController.setJwkProvider(jwkProvider);

        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String validToken = createToken("epidem_user", "RESEARCHER", TRUSTED_ISSUER, CLIENT_ID, futureExpiry, "valid-kid");
        String tamperedToken = validToken + "tampered_bytes";

        Method fetchOidcProfileMethod = AuthController.class.getDeclaredMethod("fetchOidcProfile", String.class);
        fetchOidcProfileMethod.setAccessible(true);

        try {
            fetchOidcProfileMethod.invoke(authController, tamperedToken);
            fail("Expected ReflectionException wrapping InvalidSignatureException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertInstanceOf(InvalidSignatureException.class, cause,
                    "Expected InvalidSignatureException constructive proof when token signature is tampered");
        }
    }

    @Test
    @DisplayName("Given AuthController.fetchOidcProfile executed with invalid issuer or audience claim, Then throws specific InvalidClaimException")
    void testAuthController_FetchOidcProfile_ThrowsInvalidClaimException() throws Exception {
        AuthController authController = new AuthController(null, null, null, null, null, null);
        authController.setJwkProvider(jwkProvider);

        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        String tokenBadIssuer = createToken("epidem_user", "RESEARCHER", "https://untrusted-provider.org", CLIENT_ID, futureExpiry, "valid-kid");

        Method fetchOidcProfileMethod = AuthController.class.getDeclaredMethod("fetchOidcProfile", String.class);
        fetchOidcProfileMethod.setAccessible(true);

        try {
            fetchOidcProfileMethod.invoke(authController, tokenBadIssuer);
            fail("Expected ReflectionException wrapping InvalidClaimException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertInstanceOf(InvalidClaimException.class, cause,
                    "Expected InvalidClaimException constructive proof when issuer claim is invalid");
        }
    }
}
