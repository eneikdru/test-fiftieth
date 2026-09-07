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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordRecoveryTokenRepository recoveryTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService.PasswordEncoderConfig passwordEncoderConfig;

    @Autowired
    private com.eneik.epidemiology.telemetry.TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private AuthController authController;

    @Autowired
    private ObjectMapper objectMapper;

    private org.springframework.test.web.client.MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = org.springframework.test.web.client.MockRestServiceServer.createServer(authController.getRestTemplate());
        telemetryEventRepository.deleteAll();
        recoveryTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Given valid registration request, When register endpoint called, Then user is created and 201 response returned")
    void testRegisterUser_Success() throws Exception {
        String regBody = "{" +
                "\"username\":\"ivanov_ii\"," +
                "\"password\":\"StrongP@ssword2026!\"," +
                "\"email\":\"ivanov@epidemiology-inst.ru\"," +
                "\"full_name\":\"Иванов Иван Иванович\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Регистрация успешно завершена.")))
                .andExpect(jsonPath("$.user.username", is("ivanov_ii")))
                .andExpect(jsonPath("$.user.email", is("ivanov@epidemiology-inst.ru")))
                .andExpect(jsonPath("$.user.full_name", is("Иванов Иван Иванович")))
                .andExpect(jsonPath("$.user.role", is("USER")));

        User created = userService.findByUsername("ivanov_ii").orElseThrow();
        assert userService.verifyPassword("StrongP@ssword2026!", created.getPasswordHash());
    }

    @Test
    @DisplayName("Given existing username or email, When register endpoint called with duplicate, Then returns 409 Conflict")
    void testRegisterUser_DuplicateUsernameOrEmail_ReturnsConflict() throws Exception {
        userService.createUser("ivanov_ii", "Pass123!", "ivanov@epidemiology-inst.ru", "Иванов Иван", "USER");

        String duplicateBody = "{" +
                "\"username\":\"ivanov_ii\"," +
                "\"password\":\"StrongP@ss2026!\"," +
                "\"email\":\"newemail@epidemiology-inst.ru\"," +
                "\"full_name\":\"Иванов Иван Второй\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code", is("USER_ALREADY_EXISTS")))
                .andExpect(jsonPath("$.message", is("Пользователь с таким именем или email уже существует.")));
    }

    @Test
    @DisplayName("Given missing registration parameters, When register endpoint called, Then returns 400 Bad Request")
    void testRegisterUser_MissingFields_ReturnsBadRequest() throws Exception {
        String invalidBody = "{\"username\":\"incomplete_user\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("Given valid LTI launch request via JSON body, When POST /api/v1/auth/lti/launch called, Then authenticates user and returns session tokens")
    void testLtiLaunch_JsonPayload_Success() throws Exception {
        String ltiJson = "{" +
                "\"username\":\"json_lti_user\"," +
                "\"full_name\":\"Иван Провайдеров\"," +
                "\"email\":\"json_lti@epidemiology-inst.ru\"," +
                "\"roles\":\"Learner\"," +
                "\"department\":\"Вирусология\"," +
                "\"oauth_signature\":\"valid_lti_signature\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ltiJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("json_lti_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")))
                .andExpect(jsonPath("$.user.department", is("Вирусология")));

        User user = userService.findByUsername("json_lti_user").orElseThrow();
        assert "RESEARCHER".equals(user.getRole());
        assert "Вирусология".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given valid login credentials with email, When login endpoint called, Then returns JWT tokens and user info")
    void testLogin_ByEmail_ReturnsAuthTokens() throws Exception {
        userService.createUser("katya_exp", "KatyaPass123!", "katya@inst.ru", "Екатерина Сергеевна", "RESEARCHER");

        String loginBody = "{\"username\":\"katya@inst.ru\",\"password\":\"KatyaPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("katya_exp")))
                .andExpect(jsonPath("$.user.email", is("katya@inst.ru")))
                .andExpect(jsonPath("$.user.full_name", is("Екатерина Сергеевна")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));

        long fallbackEvents = telemetryEventRepository.findAll().stream()
                .filter(e -> "fallback_login_success".equals(e.getEventType()) && "katya_exp".equals(e.getQueryTerm()))
                .count();
        assert fallbackEvents == 1;
    }

    @Test
    @DisplayName("Given Moodle SSO config request, When GET /api/v1/auth/moodle/config called, Then returns OAuth2 authorization URL")
    void testGetMoodleConfig_ReturnsOAuth2AuthorizationUrl() throws Exception {
        mockMvc.perform(post("/api/v1/auth/moodle/config"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/moodle/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_url", notNullValue()))
                .andExpect(jsonPath("$.auth_url", notNullValue()));
    }

    @Test
    @DisplayName("Given a Moodle role containing 'Аспирант', When mapMoodleRole is evaluated, Then it returns the 'RESEARCHER' internal role.")
    void testMoodleCallback_AspirantRole_MapsToResearcher() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_moodle_aspirant_code"))
                .andExpect(request -> {
                    if (request.getURI().getQuery() != null && request.getURI().getQuery().contains("code=")) {
                        throw new AssertionError("Token leaked in query parameter: " + request.getURI());
                    }
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"moodle_aspirant\",\"moodle_role\":\"Аспирант первого года\",\"department\":\"Эпидемиология\",\"email\":\"aspirant@inst.ru\",\"full_name\":\"Аспирант Тестович\",\"courses\":\"BIO-101\"}",
                        MediaType.APPLICATION_JSON));

        userService.createUser("moodle_aspirant", "Pass123!", "aspirant@inst.ru", "Аспирант Тестович", "USER");

        String callbackBody = "{\"code\":\"mock_moodle_aspirant_code\",\"state\":\"test_state\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_aspirant")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));

        User user = userService.findByUsername("moodle_aspirant").orElseThrow();
        assert "RESEARCHER".equals(user.getRole());
        assert "Эпидемиология".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given an OIDC SSO request, When the mock responds with valid profile data, Then the user is successfully logged in via OIDC")
    void testOidcLogin_Success() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_oidc_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"oidc_user\",\"moodle_role\":\"Исследователь\",\"department\":\"Lab\",\"email\":\"oidc@inst.ru\",\"full_name\":\"OIDC User\",\"courses\":\"\"}",
                        MediaType.APPLICATION_JSON));

        String ssoBody = "{\"username\":\"oidc_user\",\"oidc_token\":\"mock_oidc_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("oidc_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));

        long ssoEvents = telemetryEventRepository.findAll().stream()
                .filter(e -> "sso_login_success".equals(e.getEventType()) && "oidc_user".equals(e.getQueryTerm()))
                .count();
        assert ssoEvents == 1;

        User user = userService.findByUsername("oidc_user").orElseThrow();
        assert "RESEARCHER".equals(user.getRole());
        assert "Lab".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given Moodle SSO callback with valid auth code, When callback endpoint called, Then exchanges code for profile and authenticates user")
    void testMoodleCallback_ValidCode_AuthenticatesAndSyncsRole() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_moodle_auth_code"))
                .andExpect(request -> {
                    if (request.getURI().getQuery() != null && request.getURI().getQuery().contains("code=")) {
                        throw new AssertionError("Token leaked in query parameter: " + request.getURI());
                    }
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"moodle_user\",\"moodle_role\":\"Старший научный сотрудник\",\"department\":\"Эпидемиология\",\"email\":\"moodle@inst.ru\",\"full_name\":\"Moodle User\",\"courses\":\"BIO-101\"}",
                        MediaType.APPLICATION_JSON));

        userService.createUser("moodle_user", "Pass123!", "moodle@inst.ru", "Moodle User", "USER");

        String callbackBody = "{\"code\":\"mock_moodle_auth_code\",\"state\":\"test_state\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));

        User user = userService.findByUsername("moodle_user").orElseThrow();
        assert "EPIDEMIOLOGIST".equals(user.getRole());
        assert "Эпидемиология".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given external LMS is down and auth code invalid, When callback endpoint called with fallback credentials, Then authenticates locally via fallback")
    void testMoodleCallback_LmsDown_FallbackAuthenticationSuccess() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer invalid_code"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        userService.createUser("moodle_user", "MySecureFallback!", "moodle@inst.ru", "Moodle User", "USER");

        String callbackBody = "{\"code\":\"invalid_code\",\"username\":\"moodle_user\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_user")));

        long fallbackEvents = telemetryEventRepository.findAll().stream()
                .filter(e -> "fallback_login_success".equals(e.getEventType()) && "moodle_user".equals(e.getQueryTerm()))
                .count();
        assert fallbackEvents == 1;
    }

    @Test
    @DisplayName("Given valid SSO login request, When moodle sso endpoint called, Then returns JWT tokens and records sso login telemetry")
    void testSsoLogin_ReturnsAuthTokensAndRecordsTelemetry() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_valid_moodle_token"))
                .andExpect(request -> {
                    if (request.getURI().getQuery() != null && request.getURI().getQuery().contains("code=")) {
                        throw new AssertionError("Token leaked in query parameter: " + request.getURI());
                    }
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"moodle_user\",\"moodle_role\":\"Старший научный сотрудник\",\"department\":\"Эпидемиология\",\"email\":\"moodle@inst.ru\",\"full_name\":\"Moodle User\",\"courses\":\"BIO-101\"}",
                        MediaType.APPLICATION_JSON));

        userService.createUser("moodle_user", "Pass123!", "moodle@inst.ru", "Moodle User", "USER");

        String ssoBody = "{\"username\":\"moodle_user\",\"moodle_token\":\"mock_valid_moodle_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));

        long ssoEvents = telemetryEventRepository.findAll().stream()
                .filter(e -> "sso_login_success".equals(e.getEventType()) && "moodle_user".equals(e.getQueryTerm()))
                .count();
        assert ssoEvents == 1;

        User user = userService.findByUsername("moodle_user").orElseThrow();
        assert "EPIDEMIOLOGIST".equals(user.getRole());
        assert "Эпидемиология".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given LMS is unreachable, When moodle sso endpoint is called with fallback password, Then user is authenticated locally")
    void testSsoLogin_LmsUnreachable_FallbacksToLocalAuth() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_invalid_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        userService.createUser("moodle_user", "MySecureFallback!", "moodle@inst.ru", "Moodle User", "USER");

        String ssoBody = "{\"username\":\"moodle_user\",\"moodle_token\":\"mock_invalid_token\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_user")));

        long fallbackEvents = telemetryEventRepository.findAll().stream()
                .filter(e -> "fallback_login_success".equals(e.getEventType()) && "moodle_user".equals(e.getQueryTerm()))
                .count();
        assert fallbackEvents == 1;
    }

    @Test
    @DisplayName("Given SSO login request for new user, When moodle sso endpoint called, Then user is auto-provisioned with fallback password")
    void testSsoLogin_NewUserAutoProvisioning() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_valid_new_moodle_token"))
                .andExpect(request -> {
                    if (request.getURI().getQuery() != null && request.getURI().getQuery().contains("code=")) {
                        throw new AssertionError("Token leaked in query parameter: " + request.getURI());
                    }
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"new_moodle_user\",\"moodle_role\":\"Администратор\",\"department\":\"IT\",\"email\":\"new_moodle@inst.ru\",\"full_name\":\"New Moodle Admin\",\"courses\":\"\"}",
                        MediaType.APPLICATION_JSON));

        String ssoBody = "{\"username\":\"new_moodle_user\",\"moodle_token\":\"mock_valid_new_moodle_token\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("new_moodle_user")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")))
                .andExpect(jsonPath("$.user.email", is("new_moodle@inst.ru")));

        User user = userService.findByUsername("new_moodle_user").orElseThrow();
        assert "ADMIN".equals(user.getRole());
        assert "IT".equals(user.getDepartment());
        assert "new_moodle_user".equals(user.getMoodleId());

        assert userService.verifyPassword("MySecureFallback!", user.getPasswordHash());
    }

    @Test
    @DisplayName("Given invalid credentials, When login endpoint called, Then returns 401 Unauthorized")
    void testLogin_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        String invalidLogin = "{\"username\":\"unknown_user\",\"password\":\"WrongPassword!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("Given invalid SSO token and no fallback, When sso endpoint called, Then returns 401 Unauthorized")
    void testSsoLogin_InvalidTokenNoFallback_ReturnsUnauthorized() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer bad_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest());

        String ssoBody = "{\"username\":\"unknown_user\",\"moodle_token\":\"bad_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));
    }

    @Test
    @DisplayName("Given valid refresh token, When refresh endpoint called, Then issues new token pair")
    void testRefreshToken_Success() throws Exception {
        userService.createUser("refresh_user", "RefPass123!", "USER");
        String refreshToken = "ref_refresh_user_" + System.currentTimeMillis();

        String refreshBody = String.format("{\"refresh_token\":\"%s\"}", refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("refresh_user")));
    }

    @Test
    @DisplayName("Given valid logout request, When logout endpoint called, Then invalidates session and returns success")
    void testLogout_Success() throws Exception {
        String logoutBody = "{\"refresh_token\":\"ref_user1_12345\"}";

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Успешный выход из системы.")));
    }

    @Test
    @DisplayName("Given a user requests password reset with valid identity, When recovery endpoint is called, Then secure recovery link is generated and sent")
    void testRequestPasswordRecovery_GeneratesSecureRecoveryLink() throws Exception {
        User user = userService.createUser("petrov_sm", "Pass12345!", "petrov@inst.ru", "Петров С.М.", "RESEARCHER");

        // Verify service logic with explicit fixed clock and seedable random
        Instant fixedInstant = Instant.parse("2026-08-22T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        Random fixedRandom = new Random(12345L);

        PasswordRecoveryService customRecoveryService = new PasswordRecoveryService(
                userRepository,
                recoveryTokenRepository,
                passwordEncoderConfig.passwordEncoder(),
                fixedClock,
                fixedRandom,
                "http://localhost:8080"
        );

        PasswordRecoveryService.RecoveryResponse response = customRecoveryService.initiateRecovery("petrov@inst.ru");

        assert response.recoveryLink().contains("/reset-password?token=rec_tok_");
        assert response.message().equals("Инструкции по восстановлению пароля отправлены на ваш электронный адрес.");

        // Execute via MockMvc with email identity
        String requestBody = "{\"identity\":\"petrov@inst.ru\"}";

        mockMvc.perform(post("/api/v1/auth/recovery/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recovery_id", notNullValue()))
                .andExpect(jsonPath("$.recovery_token", notNullValue()))
                .andExpect(jsonPath("$.recovery_link", notNullValue()))
                .andExpect(jsonPath("$.message", is("Инструкции по восстановлению пароля отправлены на ваш электронный адрес.")));
    }

    @Test
    @DisplayName("Given a valid password reset token, When reset endpoint is called, Then user password is updated")
    void testConfirmPasswordReset_ResetsUserPassword() throws Exception {
        User user = userService.createUser("sidorov_v", "OldPassword1!", "USER");

        String requestBody = "{\"identity\":\"sidorov_v\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/recovery/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
        String recoveryToken = (String) responseMap.get("recovery_token");

        String resetBody = String.format(
                "{\"recovery_token\":\"%s\",\"new_password\":\"NewStrongPass2026!\"}",
                recoveryToken
        );

        mockMvc.perform(post("/api/v1/auth/recovery/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Пароль успешно изменен.")));

        User updatedUser = userService.findByUsername("sidorov_v").orElseThrow();
        assert userService.verifyPassword("NewStrongPass2026!", updatedUser.getPasswordHash());
    }
}
