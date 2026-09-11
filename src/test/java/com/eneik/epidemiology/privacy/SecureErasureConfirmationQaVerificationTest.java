package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Automated QA Verification test suite for Secure Erasure Confirmation.
 * Verifies that deterministic strings are rejected and secure tokens stored in the database
 * succeed and are atomically marked as used.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecureErasureConfirmationQaVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataErasureTokenRepository erasureTokenRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtAuthToken;
    private static final String TEST_USERNAME = "secure_erasure_user_qa";

    @BeforeEach
    void setUp() {
        User user = new User(TEST_USERNAME, "hashed_password_123", "RESEARCHER");
        userRepository.save(user);
        jwtAuthToken = jwtTokenProvider.generateToken(TEST_USERNAME, "RESEARCHER");
    }

    @Test
    @DisplayName("Given an erasure confirmation attempt, When using a deterministic string, Then the request is rejected")
    void givenErasureRequestWithDeterministicString_whenSubmitted_thenRejected() throws Exception {
        String deterministicToken = "CONFIRM_ERASURE_" + TEST_USERNAME;

        Map<String, Object> req = Map.of(
            "subject_id", TEST_USERNAME,
            "confirmation_token", deterministicToken,
            "reason", "152-FZ Withdrawal",
            "erasure_scope", "ALL_PERSONAL_DATA"
        );

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                .header("Authorization", "Bearer " + jwtAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_CONFIRMATION_TOKEN"))
            .andExpect(jsonPath("$.message").value("Неверный токен подтверждения удаления данных."));

        // Confirm user data still exists in database
        assertTrue(userRepository.findByUsername(TEST_USERNAME).isPresent(), "User data must not be erased when token is deterministic");
    }

    @Test
    @DisplayName("Given a valid secure token, When submitted, Then the erasure is successfully confirmed and token is marked as used")
    void givenValidSecureToken_whenSubmitted_thenErasureIsSuccessfullyConfirmed() throws Exception {
        String secureTokenString = "SECURE_CRYPTO_TOKEN_qa_8832_abc";

        DataErasureToken tokenEntity = new DataErasureToken();
        tokenEntity.setSubjectId(TEST_USERNAME);
        tokenEntity.setToken(secureTokenString);
        tokenEntity.setCreatedAt(OffsetDateTime.now());
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusHours(24));
        tokenEntity.setUsed(false);
        erasureTokenRepository.saveAndFlush(tokenEntity);

        Map<String, Object> req = Map.of(
            "subject_id", TEST_USERNAME,
            "confirmation_token", secureTokenString,
            "reason", "152-FZ Withdrawal",
            "erasure_scope", "ALL_PERSONAL_DATA"
        );

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                .header("Authorization", "Bearer " + jwtAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.request_id").exists())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.records_erased_count").value(1));

        // Confirm user is permanently erased
        assertTrue(userRepository.findByUsername(TEST_USERNAME).isEmpty(), "User data must be erased on valid secure token submission");

        // Confirm token is atomically marked as used in the database
        Optional<DataErasureToken> updatedTokenOpt = erasureTokenRepository.findByToken(secureTokenString);
        assertTrue(updatedTokenOpt.isPresent());
        assertTrue(updatedTokenOpt.get().getUsed(), "Token must be marked as used following successful erasure confirmation");
    }

    @Test
    @DisplayName("Given an already used or expired secure token, When submitted, Then the request is rejected")
    void givenUsedOrExpiredSecureToken_whenSubmitted_thenRejected() throws Exception {
        // 1. Used token
        String usedTokenStr = "SECURE_TOKEN_USED_1111";
        DataErasureToken usedToken = new DataErasureToken();
        usedToken.setSubjectId(TEST_USERNAME);
        usedToken.setToken(usedTokenStr);
        usedToken.setCreatedAt(OffsetDateTime.now().minusHours(2));
        usedToken.setExpiresAt(OffsetDateTime.now().plusHours(22));
        usedToken.setUsed(true);
        erasureTokenRepository.saveAndFlush(usedToken);

        Map<String, Object> reqUsed = Map.of(
            "subject_id", TEST_USERNAME,
            "confirmation_token", usedTokenStr,
            "reason", "152-FZ Withdrawal"
        );

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                .header("Authorization", "Bearer " + jwtAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqUsed)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_CONFIRMATION_TOKEN"));

        // 2. Expired token
        String expiredTokenStr = "SECURE_TOKEN_EXPIRED_2222";
        DataErasureToken expiredToken = new DataErasureToken();
        expiredToken.setSubjectId(TEST_USERNAME);
        expiredToken.setToken(expiredTokenStr);
        expiredToken.setCreatedAt(OffsetDateTime.now().minusHours(24));
        expiredToken.setExpiresAt(OffsetDateTime.now().minusMinutes(10));
        expiredToken.setUsed(false);
        erasureTokenRepository.saveAndFlush(expiredToken);

        Map<String, Object> reqExpired = Map.of(
            "subject_id", TEST_USERNAME,
            "confirmation_token", expiredTokenStr,
            "reason", "152-FZ Withdrawal"
        );

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                .header("Authorization", "Bearer " + jwtAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqExpired)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_CONFIRMATION_TOKEN"));
    }
}
