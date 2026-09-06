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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MoodleRoleOverrideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = userService.createUser("admin_override_tester", "AdminPass123!", "ADMIN");
        adminToken = jwtTokenProvider.generateToken(adminUser.getUsername(), adminUser.getRole());

        regularUser = userService.createUser("researcher_tester", "UserPass123!", "RESEARCHER");
        userToken = jwtTokenProvider.generateToken(regularUser.getUsername(), regularUser.getRole());
    }

    @Test
    @DisplayName("Given an unauthenticated request to /auth/moodle/override-role, When called, Then returns 401 Unauthorized")
    void givenUnauthenticatedUser_whenGetOrPostOverrideRole_thenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + regularUser.getId() + ",\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Given a non-admin authenticated user, When requesting role overrides, Then returns 403 Forbidden")
    void givenNonAdminUser_whenGetOrPostOverrideRole_thenReturns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + regularUser.getId() + ",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Given an authenticated administrator, When requesting Moodle role hierarchy mappings, Then returns 200 OK and mapping array")
    void givenAdminUser_whenGetMoodleRoleHierarchyMappings_thenReturns200OKAndMappingsList() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings", notNullValue()))
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Given an authenticated administrator, When submitting a user role override via userId, Then updates the user role and returns 200 OK")
    void givenAdminUser_whenPostUserRoleOverride_thenUpdatesUserRoleAndReturns200OK() throws Exception {
        String payload = "{\"userId\":" + regularUser.getId() + ",\"role\":\"EPIDEMIOLOGIST\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user_id", is(regularUser.getId().intValue())))
                .andExpect(jsonPath("$.role", is("EPIDEMIOLOGIST")));

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals("EPIDEMIOLOGIST", updatedUser.getRole());
    }

    @Test
    @DisplayName("Given an authenticated administrator, When submitting a user role override with snake_case user_id, Then updates the user role")
    void givenAdminUser_whenPostUserRoleOverrideSnakeCase_thenUpdatesUserRole() throws Exception {
        String payload = "{\"user_id\":" + regularUser.getId() + ",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals("ADMIN", updatedUser.getRole());
    }

    @Test
    @DisplayName("Given an authenticated administrator, When submitting a role mapping pattern update, Then updates moodle_role_mappings table")
    void givenAdminUser_whenPostNewRoleMappingPattern_thenUpdatesMoodleRoleMappingsAndReturns200OK() throws Exception {
        String payload = "{\"moodle_role_pattern\":\"лаборант\",\"internal_role\":\"RESEARCHER\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.mappings", notNullValue()));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM moodle_role_mappings WHERE moodle_role_pattern = 'лаборант' AND internal_role = 'RESEARCHER'",
                Integer.class
        );
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Given an invalid request without required fields, When posting role override, Then returns 400 Bad Request")
    void givenAdminUser_whenPostInvalidRoleOverrideRequest_thenReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_OVERRIDE_REQUEST")));
    }

    @Test
    @DisplayName("Given a non-existent user ID, When posting role override, Then returns 404 Not Found")
    void givenAdminUser_whenPostRoleOverrideForNonExistentUser_thenReturns404NotFound() throws Exception {
        String payload = "{\"userId\":999999,\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code", is("USER_NOT_FOUND")));
    }
}
