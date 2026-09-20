package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@Transactional
public class MoodleSsoAccessControlSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthController authController;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(authController.getRestTemplate());
    }

    @Test
    @DisplayName("Given valid Moodle SSO credentials and token, When POST /api/v1/auth/sso/moodle, Then returns 200 OK with access token and user claims")
    void testMoodleSsoLogin_ValidToken_Returns200OK() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer valid_moodle_sso_token"))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_sso_doc\",\"moodle_role\":\"эпидемиолог\",\"department\":\"Эпидемиология\",\"email\":\"sso_doc@inst.ru\",\"full_name\":\"SSO Epidemiologist\",\"courses\":\"EPI-101\"}",
                        MediaType.APPLICATION_JSON));

        String ssoBody = "{\"username\":\"moodle_sso_doc\",\"moodle_token\":\"valid_moodle_sso_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_sso_doc")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")))
                .andExpect(jsonPath("$.user.email", is("sso_doc@inst.ru")));

        User user = userRepository.findByUsername("moodle_sso_doc").orElseThrow();
        assert "EPIDEMIOLOGIST".equals(user.getRole());
        assert "Эпидемиология".equals(user.getDepartment());
    }

    @Test
    @DisplayName("Given invalid Moodle SSO token, When POST /api/v1/auth/sso/moodle, Then returns 401 Unauthorized with auth error response")
    void testMoodleSsoLogin_InvalidToken_Returns401Unauthorized() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer invalid_moodle_sso_token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        String ssoBody = "{\"username\":\"moodle_sso_doc\",\"moodle_token\":\"invalid_moodle_sso_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));
    }

    @Test
    @DisplayName("Given missing parameters in Moodle SSO request, When POST /api/v1/auth/sso/moodle, Then returns 400 Bad Request with validation error")
    void testMoodleSsoLogin_MissingParameters_Returns400BadRequest() throws Exception {
        String ssoBody = "{\"username\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("Given Moodle server error during SSO and valid fallback password, When POST /api/v1/auth/sso/moodle, Then authenticates via fallback and returns 200 OK")
    void testMoodleSsoLogin_UnreachableServerWithFallback_Returns200OK() throws Exception {
        userService.createUser("sso_fallback_user", "FallbackPassword123!", "sso_fb@inst.ru", "Fallback User", "RESEARCHER");

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(withServerError());

        String ssoBody = "{\"username\":\"sso_fallback_user\",\"moodle_token\":\"unreachable_sso_token\",\"fallback_password\":\"FallbackPassword123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("sso_fallback_user")));
    }

    @Test
    @DisplayName("Given Moodle server error during SSO and invalid fallback password, When POST /api/v1/auth/sso/moodle, Then returns 401 Unauthorized")
    void testMoodleSsoLogin_UnreachableServerWithInvalidFallback_Returns401Unauthorized() throws Exception {
        userService.createUser("sso_fallback_user_bad", "FallbackPassword123!", "sso_fb2@inst.ru", "Fallback User Bad", "RESEARCHER");

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(withServerError());

        String ssoBody = "{\"username\":\"sso_fallback_user_bad\",\"moodle_token\":\"unreachable_sso_token\",\"fallback_password\":\"WrongPassword!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));
    }

    @Test
    @DisplayName("Given GET /api/v1/auth/moodle/config, Then returns 200 OK with OAuth2 login URL and state cookie for auth flow")
    void testGetMoodleConfig_Returns200OKWithAuthUrlAndCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_url", notNullValue()))
                .andExpect(jsonPath("$.auth_url", notNullValue()))
                .andExpect(cookie().exists("oauth2_state"));
    }

    @Test
    @DisplayName("Given LTI launch request with valid signature, When POST /api/v1/auth/lti/launch, Then authenticates user and redirects with 302 Found")
    void testLtiLaunch_ValidSignature_Returns302FoundAndSetsCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("user_id", "lti_user_01")
                .param("ext_user_username", "lti_user_01")
                .param("lis_person_name_full", "LTI Test User")
                .param("lis_person_contact_email_primary", "lti_user@inst.ru")
                .param("roles", "Learner")
                .param("oauth_signature", "valid_lti_signature"))
                .andExpect(status().isFound())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));

        User user = userRepository.findByUsername("lti_user_01").orElseThrow();
        assert "RESEARCHER".equals(user.getRole());
    }

    @Test
    @DisplayName("Given LTI launch request with invalid signature, When POST /api/v1/auth/lti/launch, Then rejects request returning 401 Unauthorized")
    void testLtiLaunch_InvalidSignature_Returns401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("user_id", "lti_user_bad")
                .param("ext_user_username", "lti_user_bad")
                .param("oauth_signature", "invalid_signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_LTI_SIGNATURE")));
    }
}
