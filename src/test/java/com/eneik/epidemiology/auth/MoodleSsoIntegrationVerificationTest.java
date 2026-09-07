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
public class MoodleSsoIntegrationVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthController authController;

    private org.springframework.test.web.client.MockRestServiceServer mockServer;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockServer = org.springframework.test.web.client.MockRestServiceServer.createServer(authController.getRestTemplate());
    }

    @Test
    @DisplayName("Given an integration test suite, When the Moodle OAuth2 mock responds with valid roles, Then the user is successfully logged in and granted appropriate archive access")
    void testMoodleSsoValidRolesArchiveAccess() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_valid_new_moodle_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"new_moodle_user\",\"moodle_role\":\"Администратор\",\"department\":\"IT\",\"email\":\"new_moodle@inst.ru\",\"full_name\":\"New Moodle Admin\",\"courses\":\"\"}",
                        MediaType.APPLICATION_JSON));

        String ssoBody = "{\"username\":\"new_moodle_user\",\"moodle_token\":\"mock_valid_new_moodle_token\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("new_moodle_user")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")))
                .andExpect(jsonPath("$.user.email", is("new_moodle@inst.ru")));

        User user = userRepository.findByUsername("new_moodle_user").orElseThrow();
        assert "ADMIN".equals(user.getRole());
        assert "IT".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given invalid or empty Moodle SSO request, When processed, Then returns 400 Bad Request")
    void testMoodleSsoInvalidRequest_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("Given invalid Moodle SSO token and no fallback password, When processed, Then returns 401 Unauthorized")
    void testMoodleSsoInvalidToken_Returns401Unauthorized() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer invalid_moodle_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        String ssoBody = "{\"username\":\"unknown_moodle_user\",\"moodle_token\":\"invalid_moodle_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));
    }

    @Test
    @DisplayName("Given external Moodle LMS offline but valid local fallback password provided, When processed, Then authenticates user with fallback")
    void testMoodleSsoLmsOffline_AutonomousFallbackSuccess() throws Exception {
        User user = new User();
        user.setUsername("fallback_moodle_user");
        user.setRole("RESEARCHER");
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("FallbackPass123!"));
        user.setCreatedAt(java.time.OffsetDateTime.now());
        userRepository.save(user);

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer offline_moodle_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        String ssoBody = "{\"username\":\"fallback_moodle_user\",\"moodle_token\":\"offline_moodle_token\",\"fallback_password\":\"FallbackPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("fallback_moodle_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));
    }

    @Test
    @DisplayName("Given unauthorized request to Moodle role override endpoint, When called, Then returns 401 Unauthorized or 403 Forbidden")
    void testMoodleRoleOverride_UnauthorizedAndForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
