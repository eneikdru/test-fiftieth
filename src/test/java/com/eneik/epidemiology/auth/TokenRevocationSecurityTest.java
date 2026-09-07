package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
public class TokenRevocationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @BeforeEach
    void setUp() {
        revokedTokenRepository.deleteAll();
    }

    @Test
    void testPositiveLogoutRevokesToken() throws Exception {
        String testUser = "test_user_positive";
        String accessToken = jwtTokenProvider.generateToken(testUser, "USER");
        String refreshToken = "ref_" + testUser + "_" + System.currentTimeMillis();

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        assertTrue(tokenRevocationService.isTokenRevoked(refreshToken), "Refresh token should be revoked");
        assertTrue(tokenRevocationService.isTokenRevoked(accessToken), "Access token should be revoked");
    }

    @Test
    void testNegativeAccessWithRevokedTokenFails() throws Exception {
        String testUser = "test_user_negative_access";
        String accessToken = jwtTokenProvider.generateToken(testUser, "USER");

        tokenRevocationService.revokeToken(accessToken);

        mockMvc.perform(get("/api/v1/dossier/documents/count")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testNegativeRefreshWithRevokedTokenFails() throws Exception {
        String testUser = "test_user_negative_refresh";
        String refreshToken = "ref_" + testUser + "_" + System.currentTimeMillis();

        tokenRevocationService.revokeToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testBoundaryMissingOrInvalidTokenHandling() throws Exception {
        // Missing token should fail gracefully
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        // Empty token should fail gracefully
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"\"}"))
                .andExpect(status().isBadRequest());

        // Invalid token signature but valid structure
        String invalidAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmYWtlX3VzZXIiLCJyb2xlIjoiVVNFUiJ9.invalid_signature_here";
        mockMvc.perform(get("/api/v1/dossier/documents/count")
                .header("Authorization", "Bearer " + invalidAccessToken))
                .andExpect(status().isUnauthorized());
    }
}
