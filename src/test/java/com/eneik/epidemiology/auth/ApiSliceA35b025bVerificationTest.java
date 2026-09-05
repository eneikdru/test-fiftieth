package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiSliceA35b025bVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Given valid SSO login, When authenticating, Then the missing test coverage module A35b025b is verified (Happy Path)")
    void testA35b025bCoverageHappyPath() throws Exception {
        String ssoBody = "{\"username\":\"new_moodle_user\",\"moodle_token\":\"mock_valid_new_moodle_token\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()));
    }

    @Test
    @DisplayName("Given invalid moodle token, When authenticating, Then return unauthorized (Negative 1)")
    void testA35b025bCoverageNegative1() throws Exception {
        String ssoBody = "{\"username\":\"new_moodle_user\",\"moodle_token\":\"mock_invalid_token\",\"fallback_password\":\"wrong_password\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given missing username, When authenticating, Then return bad request (Negative 2)")
    void testA35b025bCoverageNegative2() throws Exception {
        String ssoBody = "{\"moodle_token\":\"mock_valid_new_moodle_token\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given empty fallback password, When authenticating, Then fallback to generated (Boundary)")
    void testA35b025bCoverageBoundary() throws Exception {
        String ssoBody = "{\"username\":\"new_moodle_user\",\"moodle_token\":\"mock_valid_new_moodle_token\",\"fallback_password\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()));
    }
}
