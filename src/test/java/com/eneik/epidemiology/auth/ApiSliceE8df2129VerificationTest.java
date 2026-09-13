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
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class ApiSliceE8df2129VerificationTest {

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
    @DisplayName("Given valid registration request, When submitting user details, Then returns 201 Created")
    void testRegisterUser_Returns201Created() throws Exception {
        String regJson = "{"
                + "\"username\":\"slice_user_2026\","
                + "\"password\":\"SecurePassword123!\","
                + "\"email\":\"slice_user@epidemiology-inst.ru\","
                + "\"full_name\":\"Test Slice User\""
                + "}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("slice_user_2026")));
    }

    @Test
    @DisplayName("Given registered user credentials, When logging in, Then returns 200 OK with access token")
    void testLoginUser_Returns200WithAccessToken() throws Exception {
        userService.createUser("login_user_2026", "Password123!", "login_user@inst.ru", "Login User", "USER");

        String loginJson = "{\"username\":\"login_user_2026\",\"password\":\"Password123!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("login_user_2026")));
    }

    @Test
    @DisplayName("Given an authenticated client with valid Bearer token, When accessing profile, Then receives 200 OK response instead of 401")
    void testAuthenticatedProfileRequest_Returns200OK() throws Exception {
        User user = userService.createUser("auth_profile_user", "Password123!", "auth_profile@inst.ru", "Auth Profile User", "USER");
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("auth_profile_user")))
                .andExpect(jsonPath("$.user.role", is("USER")));
    }

    @Test
    @DisplayName("Given an authenticated client with valid Bearer token, When searching dossier documents, Then receives 200 OK response")
    void testAuthenticatedDossierSearch_Returns200OK() throws Exception {
        User user = userService.createUser("dossier_auth_user", "Password123!", "dossier@inst.ru", "Dossier User", "RESEARCHER");
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        mockMvc.perform(get("/api/v1/dossier/documents")
                .param("query", "Иванов")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given login flow, When user authenticates via Moodle SSO, Then it correctly integrates with Moodle SSO and returns 200 OK with access token")
    void testMoodleSsoIntegration_Returns200OKAndAuthTokens() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer moodle_sso_valid_token"))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_sso_doc\",\"moodle_role\":\"Исследователь\",\"department\":\"Кафедра Эпидемиологии\",\"email\":\"moodle_doc@inst.ru\",\"full_name\":\"Д-р Иванов\",\"courses\":\"EPID-101\"}",
                        MediaType.APPLICATION_JSON));

        String ssoJson = "{\"username\":\"moodle_sso_doc\",\"moodle_token\":\"moodle_sso_valid_token\",\"fallback_password\":\"FallbackPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_sso_doc")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")))
                .andExpect(jsonPath("$.user.department", is("Кафедра Эпидемиологии")));
    }

    @Test
    @DisplayName("Given an unauthenticated request to profile endpoint, Then receives 401 Unauthorized")
    void testUnauthenticatedProfileRequest_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
