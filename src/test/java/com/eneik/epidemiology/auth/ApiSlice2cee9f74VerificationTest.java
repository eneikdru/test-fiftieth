package com.eneik.epidemiology.auth;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class ApiSlice2cee9f74VerificationTest {

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

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(authController.getRestTemplate());
    }

    @Test
    @DisplayName("Given unauthenticated or invalid token request, When accessing protected endpoints, Then returns 401 Unauthorized")
    void testProtectedEndpoints_UnauthenticatedRequest_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer invalid_token_xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given valid Moodle OAuth2 token, When ssoLogin executed, Then syncs Moodle role hierarchy and authenticates user")
    void testMoodleSsoLogin_ValidToken_Returns200OKAndMapsRole() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer moodle_2cee_token"))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_2cee_researcher\",\"moodle_role\":\"исследователь\",\"department\":\"Эпидемиология\",\"email\":\"2cee@inst.ru\",\"full_name\":\"Test Researcher\",\"courses\":\"EPID-101\"}",
                        MediaType.APPLICATION_JSON));

        String ssoJson = "{\"username\":\"moodle_2cee_researcher\",\"moodle_token\":\"moodle_2cee_token\",\"fallback_password\":\"Fallback123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_2cee_researcher")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")))
                .andExpect(jsonPath("$.user.department", is("Эпидемиология")));

        mockServer.verify();
    }

    @Test
    @DisplayName("Given invalid or unmapped Moodle credentials, When ssoLogin executed, Then strictly denies access and returns 401 Unauthorized")
    void testMoodleSsoLogin_InvalidToken_StrictlyDeniedWith401Unauthorized() throws Exception {
        String invalidSsoJson = "{\"username\":\"moodle_unmapped_user\",\"moodle_token\":\"invalid_unmapped_token\"}";

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidSsoJson))
                .andExpect(status().isUnauthorized());

        mockServer.verify();
    }

    @Test
    @DisplayName("Given Moodle server unreachable, When ssoLogin executed with valid fallback password, Then uses autonomous fallback and returns 200 OK")
    void testMoodleSsoLogin_UnreachableMoodleServer_AutonomousFallbackSuccess() throws Exception {
        User user = userService.createUserWithMoodle(
                "fallback_2cee_user",
                "FallbackSecret123!",
                "fallback2cee@inst.ru",
                "Fallback User",
                "USER",
                "moodle_fallback_2cee",
                "Вирусология",
                "VIR-101"
        );

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        String ssoJson = "{\"username\":\"fallback_2cee_user\",\"moodle_token\":\"invalid_token\",\"fallback_password\":\"FallbackSecret123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("fallback_2cee_user")));

        mockServer.verify();
    }

    @Test
    @DisplayName("Given Moodle server unreachable and invalid fallback password, When ssoLogin executed, Then strictly denies access with 401 Unauthorized")
    void testMoodleSsoLogin_UnreachableMoodleServer_InvalidFallback_Returns401Unauthorized() throws Exception {
        userService.createUserWithMoodle(
                "fallback_denied_user",
                "CorrectSecret123!",
                "denied@inst.ru",
                "Denied User",
                "USER",
                "moodle_denied",
                "Вирусология",
                "VIR-101"
        );

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        String ssoJson = "{\"username\":\"fallback_denied_user\",\"moodle_token\":\"invalid_token\",\"fallback_password\":\"WrongSecret123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoJson))
                .andExpect(status().isUnauthorized());

        mockServer.verify();
    }

    @Test
    @DisplayName("Given ADMIN user, When GET /api/v1/auth/moodle/override-role called, Then returns Moodle role hierarchy mappings list")
    void testGetMoodleRoleMappings_AdminAccess_Returns200OK() throws Exception {
        User admin = userService.createUser("admin_2cee", "AdminPass123!", "admin_2cee@inst.ru", "Admin User", "ADMIN");
        String adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());

        mockMvc.perform(get("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }
}
