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
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@AutoConfigureMockMvc
@Transactional
class PrivacyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        User testUser = new User("privacy_api_user", "hashed_pass", "RESEARCHER");
        testUser = userRepository.save(testUser);
        token = jwtTokenProvider.generateToken("privacy_api_user", "RESEARCHER");
    }

    @Test
    @DisplayName("Given valid export request, When POST /api/v1/privacy/export-requests, Then 202 Accepted and job response returned")
    void testCreateDataExportRequest() throws Exception {
        Map<String, Object> req = Map.of(
            "subject_id", "privacy_api_user",
            "requested_format", "ZIP",
            "notes", "Export request"
        );

        mockMvc.perform(post("/api/v1/privacy/export-requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.request_id").exists())
            .andExpect(jsonPath("$.subject_id").value("privacy_api_user"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.download_url").exists());
    }

    @Test
    @DisplayName("Given valid erasure request, When POST /api/v1/privacy/erasure-requests, Then 202 Accepted and user permanently deleted")
    void testCreateDataErasureRequest() throws Exception {
        Map<String, Object> req = Map.of(
            "subject_id", "privacy_api_user",
            "confirmation_token", "CONFIRM_ERASURE_privacy_api_user",
            "reason", "152-FZ Consent Withdrawal",
            "erasure_scope", "ALL_PERSONAL_DATA"
        );

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.request_id").exists())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.records_erased_count").value(1));
    }

    @Test
    @DisplayName("Given invalid confirmation token, When POST /api/v1/privacy/erasure-requests, Then 400 Bad Request returned with Russian message")
    void testCreateDataErasureInvalidToken() throws Exception {
        Map<String, Object> req = Map.of(
            "subject_id", "privacy_api_user",
            "confirmation_token", "BAD_TOKEN",
            "reason", "152-FZ",
            "erasure_scope", "ALL_PERSONAL_DATA"
        );

        mockMvc.perform(post("/api/v1/privacy/erasure-requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_CONFIRMATION_TOKEN"))
            .andExpect(jsonPath("$.message").value("Неверный токен подтверждения удаления данных."));
    }
}
