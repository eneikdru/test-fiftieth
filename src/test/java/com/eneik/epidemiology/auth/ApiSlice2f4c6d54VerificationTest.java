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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class ApiSlice2f4c6d54VerificationTest {

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
    @DisplayName("Given a valid authenticated request, When it accesses an endpoint requiring 200/201, Then the server responds successfully")
    void testAuthenticatedRequest_Returns200OK() throws Exception {
        User user = userService.createUser("authenticated_user", "Pass123!", "auth_user@example.com", "Auth User", "RESEARCHER");
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getDepartment(), user.getCourses());

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("authenticated_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));
    }

    @Test
    @DisplayName("Given a user attempting to log in, When using Moodle credentials, Then SSO authenticates them and synchronizes their roles")
    void testMoodleSsoLoginAndRoleSynchronization() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer mock_moodle_token_2f4c"))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_sync_user\",\"moodle_role\":\"Старший научный сотрудник\",\"department\":\"Epidemiology\",\"email\":\"sync_user@inst.ru\",\"full_name\":\"Sync User\",\"courses\":\"BioStat\"}",
                        MediaType.APPLICATION_JSON));

        String ssoBody = "{\"username\":\"moodle_sync_user\",\"moodle_token\":\"mock_moodle_token_2f4c\",\"fallback_password\":\"FallbackPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_sync_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")))
                .andExpect(jsonPath("$.user.department", is("Epidemiology")));

        User syncedUser = userRepository.findByUsername("moodle_sync_user").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("EPIDEMIOLOGIST", syncedUser.getRole());
        org.junit.jupiter.api.Assertions.assertEquals("Epidemiology", syncedUser.getDepartment());
    }

    @Test
    @DisplayName("Given a Moodle login failure, When the offline fallback is used, Then the user is granted access based on fallback rules")
    void testMoodleLoginFailure_OfflineFallbackGrantsAccess() throws Exception {
        userService.createUser("offline_fallback_user", "FallbackSecret123!", "offline@inst.ru", "Offline User", "USER");

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andRespond(withServerError());

        String ssoBody = "{\"username\":\"offline_fallback_user\",\"moodle_token\":\"unreachable_token_2f4c\",\"fallback_password\":\"FallbackSecret123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("offline_fallback_user")));
    }
}
