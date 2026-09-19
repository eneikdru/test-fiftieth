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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase(type = io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
public class ApiSlice2c5631faVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthController authController;

    private KeyPair rsaKeyPair;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        rsaKeyPair = kpg.generateKeyPair();
        rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();

        JwkProvider mockJwkProvider = new JwkProvider() {
            @Override
            public Jwk get(String keyId) throws JwkException {
                if ("slice2c-kid".equals(keyId)) {
                    return Jwk.fromValues(Map.of(
                            "kid", "slice2c-kid",
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

    private String createSignedOidcToken(String kid, String username, String role, String courses, String department) {
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
                    .withClaim("department", department)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test OIDC token", e);
        }
    }

    @Test
    @DisplayName("Given valid registration request, When submitting user details, Then returns 201 Created")
    void testRegisterUser_Returns201Created() throws Exception {
        String regJson = "{"
                + "\"username\":\"slice2c_reg_user\","
                + "\"password\":\"SecurePass2026!\","
                + "\"email\":\"slice2c_reg@epidemiology-inst.ru\","
                + "\"full_name\":\"Slice 2c Registered User\""
                + "}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("slice2c_reg_user")));
    }

    @Test
    @DisplayName("Given an authenticated client with valid Bearer token, When accessing profile, Then receives 200 OK response")
    void testAuthenticatedProfileRequest_Returns200OK() throws Exception {
        User user = userService.createUser("slice2c_auth_user", "Password123!", "slice2c_auth@inst.ru", "Slice Auth User", "RESEARCHER");
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getDepartment(), user.getCourses());

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("slice2c_auth_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));
    }

    @Test
    @DisplayName("Given valid OIDC SSO token, When authenticating, Then user profile and role mapping return 200 OK with mapped role")
    void testMoodleOidcSsoRoleMapping_Returns200OKAndAssignsRole() throws Exception {
        String oidcToken = createSignedOidcToken("slice2c-kid", "moodle_2c_epidemiologist", "эпидемиолог", "EPID-501", "Кафедра Эпидемиологии");
        String ssoJson = String.format("{\"username\":\"moodle_2c_epidemiologist\",\"oidc_token\":\"%s\"}", oidcToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_2c_epidemiologist")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")))
                .andExpect(jsonPath("$.user.department", is("Кафедра Эпидемиологии")));

        User user = userRepository.findByUsername("moodle_2c_epidemiologist").orElseThrow();
        assertEquals("EPIDEMIOLOGIST", user.getRole());
        assertEquals("Кафедра Эпидемиологии", user.getDepartment());
    }
}
