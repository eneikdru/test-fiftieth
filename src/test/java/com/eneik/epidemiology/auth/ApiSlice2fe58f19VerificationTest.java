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
public class ApiSlice2fe58f19VerificationTest {

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
                + "\"username\":\"slice2fe_user\","
                + "\"password\":\"SecurePassword123!\","
                + "\"email\":\"slice2fe@epidemiology-inst.ru\","
                + "\"full_name\":\"Slice 2fe User\""
                + "}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("slice2fe_user")));
    }

    @Test
    @DisplayName("Given an authenticated client with valid Bearer token, When accessing profile, Then receives 200 OK response instead of 401")
    void testAuthenticatedProfileRequest_Returns200OK() throws Exception {
        User user = userService.createUser("auth_2fe_user", "Password123!", "auth_2fe@inst.ru", "Auth 2fe User", "USER");
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("auth_2fe_user")))
                .andExpect(jsonPath("$.user.role", is("USER")));
    }

    @Test
    @DisplayName("Given Moodle user SSO login, When logging in, Then SSO and Role Mapping correctly assign permissions and return 200 OK")
    void testMoodleSsoRoleMapping_Returns200OKAndAssignsPermissions() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer moodle_2fe_sso_token"))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_2fe_researcher\",\"moodle_role\":\"исследователь\",\"department\":\"Кафедра Эпидемиологии\",\"email\":\"moodle_2fe@inst.ru\",\"full_name\":\"Dr 2fe Researcher\",\"courses\":\"EPID-202\"}",
                        MediaType.APPLICATION_JSON));

        String ssoJson = "{\"username\":\"moodle_2fe_researcher\",\"moodle_token\":\"moodle_2fe_sso_token\",\"fallback_password\":\"FallbackPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_2fe_researcher")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")))
                .andExpect(jsonPath("$.user.department", is("Кафедра Эпидемиологии")));
    }
}
