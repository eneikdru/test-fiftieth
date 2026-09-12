package com.eneik.epidemiology.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class JwkValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private KeyPair rsaKeyPair;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        rsaKeyPair = kpg.generateKeyPair();
        rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();

        JwkProvider mockJwkProvider = new JwkProvider() {
            @Override
            public Jwk get(String keyId) throws JwkException {
                if ("valid-kid".equals(keyId)) {
                    return Jwk.fromValues(Map.of(
                        "kid", "valid-kid",
                        "kty", "RSA",
                        "alg", "RS256",
                        "use", "sig",
                        "n", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getModulus().toByteArray()),
                        "e", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getPublicExponent().toByteArray())
                    ));
                }
                throw new JwkException("Key ID not found: " + keyId);
            }
        };
        authController.setJwkProvider(mockJwkProvider);
    }

    private String createSignedOidcToken(String kid, String username, String role, String courses, Date expiresAt) {
        try {
            Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, rsaPrivateKey);
            return JWT.create()
                    .withKeyId(kid)
                    .withIssuer("https://moodle.epidemiology-inst.ru")
                    .withAudience("epidemiology_portal")
                    .withSubject(username)
                    .withClaim("username", username)
                    .withClaim("moodle_role", role)
                    .withClaim("courses", courses)
                    .withClaim("department", "Лаборатория вирусологии")
                    .withIssuedAt(new Date())
                    .withExpiresAt(expiresAt != null ? expiresAt : new Date(System.currentTimeMillis() + 3600000))
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test OIDC token", e);
        }
    }

    @Test
    @DisplayName("Given an uninitialized or broken JWK Provider URL, When JWK Provider initialization fails, Then getJwkProvider throws IllegalStateException")
    void testJwkProvider_InitializationFailure_ThrowsSystemError() {
        AuthController brokenController = new AuthController(userService, jwtTokenProvider, null, null, null, null);
        brokenController.setMoodleServerUrl("ht!!ps://invalid-url-format-causes-exception");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            brokenController.oidcLogin(new AuthController.OidcLoginRequest("test_user", "header.payload.signature", null));
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Failed to initialize JwkProvider"));
    }

    @Test
    @DisplayName("Given an OIDC token signed with a valid RSA key, When signature is cryptographically verified, Then authentication succeeds and user claims are extracted")
    void testOidcSignatureValidation_ValidSignature_AuthenticatesUser() throws Exception {
        String token = createSignedOidcToken("valid-kid", "crypto_user", "Старший научный сотрудник", "EPID-2026,EPID-2027", null);
        String ssoBody = String.format("{\"username\":\"crypto_user\",\"oidc_token\":\"%s\"}", token);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("crypto_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")))
                .andExpect(jsonPath("$.user.courses", is("EPID-2026,EPID-2027")));

        User user = userService.findByUsername("crypto_user").orElseThrow();
        assertEquals("EPIDEMIOLOGIST", user.getRole());
        assertEquals("EPID-2026,EPID-2027", user.getCourses());
    }

    @Test
    @DisplayName("Given an OIDC token with an invalid key ID or tampered signature, When signature verification fails, Then system returns 401 Unauthorized without reading claims")
    void testOidcSignatureValidation_InvalidSignature_Returns401Unauthorized() throws Exception {
        String invalidKidToken = createSignedOidcToken("unknown-kid", "attacker_user", "Администратор", "ALL", null);
        String ssoBody = String.format("{\"username\":\"attacker_user\",\"oidc_token\":\"%s\"}", invalidKidToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));

        assertTrue(userService.findByUsername("attacker_user").isEmpty(), "User with invalid OIDC token signature must not be created or authenticated");
    }

    @Test
    @DisplayName("Given an expired OIDC token, When signature and expiry are evaluated, Then system rejects token with 401 Unauthorized")
    void testOidcSignatureValidation_ExpiredToken_Returns401Unauthorized() throws Exception {
        Date expiredDate = new Date(System.currentTimeMillis() - 3600000);
        String expiredToken = createSignedOidcToken("valid-kid", "expired_user", "Исследователь", "EPID-101", expiredDate);
        String ssoBody = String.format("{\"username\":\"expired_user\",\"oidc_token\":\"%s\"}", expiredToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));
    }

    @Test
    @DisplayName("Given a user authenticates via OIDC, When profile is fetched, Then Moodle roles and course-based access control are synchronized")
    void testOidcProfile_RoleAndCourseAccessControlSynchronization() throws Exception {
        // Initial user registration with default USER role
        User existingUser = userService.createUser("sync_user", "OldPass123!", "sync_user@inst.ru", "Синхронизированный Пользователь", "USER");
        assertEquals("USER", existingUser.getRole());

        // Authenticate with OIDC token carrying Moodle role "Администратор" and new courses
        String oidcToken = createSignedOidcToken("valid-kid", "sync_user", "Администратор", "EPID-401,EPID-402", null);
        String ssoBody = String.format("{\"username\":\"sync_user\",\"oidc_token\":\"%s\"}", oidcToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("ADMIN")))
                .andExpect(jsonPath("$.user.courses", is("EPID-401,EPID-402")));

        // Verify role and courses are updated in database
        User updatedUser = userService.findByUsername("sync_user").orElseThrow();
        assertEquals("ADMIN", updatedUser.getRole());
        assertEquals("EPID-401,EPID-402", updatedUser.getCourses());
    }
}
