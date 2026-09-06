package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
class MoodleRoleOverrideTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Given unauthenticated user, When requesting role mappings, Then returns 401 Unauthorized")
    void testGetMoodleRoleMappings_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "standard_user", roles = {"USER"})
    @DisplayName("Given non-admin user, When requesting role mappings, Then returns 403 Access Denied")
    void testGetMoodleRoleMappings_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Given admin user, When requesting role mappings, Then returns hierarchy mappings array")
    void testGetMoodleRoleMappings_Admin_ReturnsHierarchyMappings() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings", notNullValue()))
                .andExpect(jsonPath("$.mappings", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "standard_user", roles = {"USER"})
    @DisplayName("Given non-admin user, When submitting role override, Then returns 403 Access Denied")
    void testOverrideMoodleRole_NonAdmin_Returns403() throws Exception {
        User targetUser = userService.createUser("target_user_1", "Pass123!", "USER");

        String overrideBody = String.format("{\"userId\": %d, \"role\": \"ADMIN\"}", targetUser.getId());

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overrideBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Given admin user and valid user ID, When submitting role override, Then updates user role and returns success")
    void testOverrideMoodleRole_AdminWithUserId_Success() throws Exception {
        User targetUser = userService.createUser("target_user_2", "Pass123!", "USER");

        String overrideBody = String.format("{\"userId\": %d, \"role\": \"EPIDEMIOLOGIST\"}", targetUser.getId());

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overrideBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("EPIDEMIOLOGIST")))
                .andExpect(jsonPath("$.user_id", is(targetUser.getId().intValue())));

        User updatedUser = userService.findByUsername("target_user_2").orElseThrow();
        assertEquals("EPIDEMIOLOGIST", updatedUser.getRole());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Given admin user and valid username, When submitting role override, Then updates user role and returns success")
    void testOverrideMoodleRole_AdminWithUsername_Success() throws Exception {
        User targetUser = userService.createUser("target_user_3", "Pass123!", "USER");

        String overrideBody = "{\"username\": \"target_user_3\", \"role\": \"ADMIN\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overrideBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        User updatedUser = userService.findByUsername("target_user_3").orElseThrow();
        assertEquals("ADMIN", updatedUser.getRole());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("Given non-existent user ID or username, When submitting role override, Then returns 400 Bad Request")
    void testOverrideMoodleRole_UserNotFound_Returns400() throws Exception {
        String overrideBody = "{\"username\": \"non_existent_user_999\", \"role\": \"ADMIN\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overrideBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("USER_NOT_FOUND")));
    }
}
