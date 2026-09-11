package com.eneik.epidemiology.auth;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class MoodleRoleSyncServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MoodleRoleSyncService moodleRoleSyncService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private com.eneik.epidemiology.security.JwtTokenProvider jwtTokenProvider;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        moodleRoleSyncService = new MoodleRoleSyncService(
                userRepository,
                userService,
                moodleRoleSyncService != null ? (org.springframework.jdbc.core.JdbcTemplate) org.springframework.test.util.ReflectionTestUtils.getField(moodleRoleSyncService, "jdbcTemplate") : null,
                restTemplate
        );
    }

    @Test
    @DisplayName("Given users with Moodle roles updated in Moodle, When syncAllUserRoles runs, Then internal roles in archive are updated")
    void givenMoodleRoleUpdate_whenSyncAllUserRolesRuns_thenArchiveRoleIsUpdated() {
        userRepository.deleteAll();

        User user1 = userService.createUserWithMoodle("sync_user_1", "Pass123!", "user1@test.ru", "User One", "USER", "moodle_101", "Dept A", "Course A");
        User user2 = userService.createUserWithMoodle("sync_user_2", "Pass123!", "user2@test.ru", "User Two", "RESEARCHER", "moodle_102", "Dept B", "Course B");

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/api/v1/users/moodle_101/role"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"moodle_role\":\"Старший научный сотрудник\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/api/v1/users/moodle_102/role"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"moodle_role\":\"Администратор\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = moodleRoleSyncService.syncAllUserRoles();

        assertEquals(true, result.get("success"));
        assertEquals(2, result.get("total_users"));
        assertEquals(2, result.get("synced_users"));
        assertEquals(2, result.get("updated_users"));

        User updated1 = userRepository.findById(user1.getId()).orElseThrow();
        User updated2 = userRepository.findById(user2.getId()).orElseThrow();

        assertEquals("EPIDEMIOLOGIST", updated1.getRole());
        assertEquals("ADMIN", updated2.getRole());

        mockServer.verify();
    }

    @Test
    @DisplayName("Given unauthenticated call to /api/v1/auth/moodle/sync-roles, When invoked, Then returns 401 Unauthorized")
    void givenUnauthenticatedCall_whenPostSyncRoles_thenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/moodle/sync-roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Given authenticated ADMIN call to /api/v1/auth/moodle/sync-roles, When invoked, Then returns 200 OK with sync summary")
    void givenAdminUser_whenPostSyncRoles_thenReturns200OK() throws Exception {
        User adminUser = userService.createUser("admin_sync_tester", "AdminPass123!", "ADMIN");
        String adminToken = jwtTokenProvider.generateToken(adminUser.getUsername(), adminUser.getRole());

        mockMvc.perform(post("/api/v1/auth/moodle/sync-roles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
