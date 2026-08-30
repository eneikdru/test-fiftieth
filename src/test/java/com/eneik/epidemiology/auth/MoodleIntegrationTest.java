package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MoodleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    // The endpoint uses an internal fetchMoodleProfile private method, so it's already doing a mock inside AuthController
    // But to truly "mock the OAuth2 mock" from the outside without editing the controller, we rely on the internal mock behavior.
    // If the Acceptance Criteria wants us to test when the "Moodle OAuth2 mock responds with valid roles"
    // we use the mock token handled by the controller logic: "mock_valid_new_moodle_token"

    @Test
    @DisplayName("Given an integration test suite, When the Moodle OAuth2 mock responds with valid roles, Then the user is successfully logged in and granted appropriate archive access.")
    public void testMoodleSSOIntegration() throws Exception {
        String ssoBody = "{\"username\":\"new_moodle_user\",\"moodle_token\":\"mock_valid_new_moodle_token\",\"fallback_password\":\"SecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("new_moodle_user")))
                .andExpect(jsonPath("$.user.role", is("ADMIN"))) // Valid roles granted (Администратор -> ADMIN)
                .andExpect(jsonPath("$.user.email", is("new_moodle@inst.ru")));

        User user = userService.findByUsername("new_moodle_user").orElseThrow();
        assertEquals("ADMIN", user.getRole());
        assertEquals("IT", user.getDepartment());
        assertEquals("new_moodle_user", user.getMoodleId());
    }
}
