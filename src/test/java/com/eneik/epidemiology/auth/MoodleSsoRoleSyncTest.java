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
public class MoodleSsoRoleSyncTest {

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
    @DisplayName("Given valid OIDC payload, When logging in, Then roles are synchronized")
    void testRoleSync() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_valid_new_moodle_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"sync_user\",\"moodle_role\":\"Администратор\",\"department\":\"IT\",\"email\":\"sync@inst.ru\",\"full_name\":\"Sync User\",\"courses\":\"\"}",
                        MediaType.APPLICATION_JSON));

        userService.createUser("sync_user", "Pass123!", "sync@inst.ru", "Sync User", "USER");

        String ssoBody = "{\"username\":\"sync_user\",\"moodle_token\":\"mock_valid_new_moodle_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("ADMIN")));

        User user = userRepository.findByUsername("sync_user").orElseThrow();
        assert "ADMIN".equals(user.getRole());
    }

    @Test
    @DisplayName("Given an authenticated ADMIN user, When invoking POST /api/v1/auth/moodle/sync-roles, Then executes role synchronization and returns 200 OK")
    void testSyncMoodleRolesEndpoint_AdminUser_Returns200OK() throws Exception {
        User admin = userService.createUser("admin_sync_tester", "AdminPass123!", "ADMIN");
        com.eneik.epidemiology.security.JwtTokenProvider jwtProvider = new com.eneik.epidemiology.security.JwtTokenProvider("default-secret-key-for-jwt-signing-2026-epidemiology-portal", 3600);
        String adminToken = jwtProvider.generateToken(admin.getUsername(), admin.getRole());

        // Create a Moodle-linked user whose role needs sync
        userService.createUserWithMoodle("moodle_sync_target", "Pass123!", "target@inst.ru", "Target User", "USER", "администратор_moodle", "IT", "EPID-101");

        mockMvc.perform(post("/api/v1/auth/moodle/sync-roles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.synced_users_count", notNullValue()));

        User updatedUser = userRepository.findByUsername("moodle_sync_target").orElseThrow();
        assert "ADMIN".equals(updatedUser.getRole());
    }

    @Test
    @DisplayName("Given a non-ADMIN user or unauthenticated request, When invoking POST /api/v1/auth/moodle/sync-roles, Then returns 403 Forbidden or 401 Unauthorized")
    void testSyncMoodleRolesEndpoint_SecurityEnforcement() throws Exception {
        // 1. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/moodle/sync-roles"))
                .andExpect(status().isUnauthorized());

        // 2. Standard RESEARCHER user -> 403 Forbidden
        User researcher = userService.createUser("researcher_sync_tester", "Pass123!", "RESEARCHER");
        com.eneik.epidemiology.security.JwtTokenProvider jwtProvider = new com.eneik.epidemiology.security.JwtTokenProvider("default-secret-key-for-jwt-signing-2026-epidemiology-portal", 3600);
        String researcherToken = jwtProvider.generateToken(researcher.getUsername(), researcher.getRole());

        mockMvc.perform(post("/api/v1/auth/moodle/sync-roles")
                .header("Authorization", "Bearer " + researcherToken))
                .andExpect(status().isForbidden());
    }
}
