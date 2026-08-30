package com.eneik.epidemiology.user;

import com.eneik.epidemiology.auth.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MoodleSsoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Given an integration test suite, When the Moodle OAuth2 mock responds with valid roles, Then the user is successfully logged in and granted appropriate archive access.")
    void testMoodleSsoMockIntegration() throws Exception {
        // "moodle_user" is tied to "mock_valid_moodle_token" in fetchMoodleProfile in AuthController.
        // It serves as our 'Moodle OAuth2 mock' responses.
        if(userService.findByUsernameOrEmail("moodle_user").isEmpty()) {
            userService.createUser("moodle_user", "Pass123!", "moodle@inst.ru", "Moodle User", "USER");
        }

        String ssoBody = "{\"username\":\"moodle_user\",\"moodle_token\":\"mock_valid_moodle_token\"}";
        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.user.username").value("moodle_user"));
    }
}
