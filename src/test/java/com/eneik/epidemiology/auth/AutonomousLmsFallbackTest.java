package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
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
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class AutonomousLmsFallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthController authController;

    private org.springframework.test.web.client.MockRestServiceServer mockServer;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockServer = org.springframework.test.web.client.MockRestServiceServer.createServer(authController.getRestTemplate());
    }

    @Test
    @DisplayName("Given LMS is unreachable, When logging in with fallback credentials, Then access is granted")
    void testLmsFallback() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        userService.createUser("fallback_user", "MySecureFallback!", "fallback@inst.ru", "Fallback User", "USER");

        String ssoBody = "{\"username\":\"fallback_user\",\"moodle_token\":\"mock_invalid_token\",\"fallback_password\":\"MySecureFallback!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("fallback_user")));
    }

    @Test
    @DisplayName("Given LMS throws LmsServerException on Moodle callback, When valid local credentials supplied, Then access is granted")
    void testLmsServerExceptionMoodleCallbackFallback() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        userService.createUser("callback_user", "FallbackPass123!", "callback@inst.ru", "Callback Fallback User", "USER");

        String callbackBody = "{\"code\":\"invalid_code\",\"state\":\"valid_state\",\"username\":\"callback_user\",\"fallback_password\":\"FallbackPass123!\"}";

        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("oauth2_state", "valid_state");

        mockMvc.perform(post("/api/v1/auth/moodle/callback")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("callback_user")));
    }

    @Test
    @DisplayName("Given LMS throws LmsServerException on OIDC login, When valid local credentials supplied, Then access is granted")
    void testLmsServerExceptionOidcFallback() throws Exception {
        userService.createUser("oidc_user", "OidcPass123!", "oidc@inst.ru", "OIDC Fallback User", "USER");

        String oidcBody = "{\"username\":\"oidc_user\",\"oidc_token\":\"invalid.jwt.token\",\"fallback_password\":\"OidcPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(oidcBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("oidc_user")));
    }
}
