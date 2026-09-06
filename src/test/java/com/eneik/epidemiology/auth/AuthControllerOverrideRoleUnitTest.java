package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import com.eneik.epidemiology.telemetry.TelemetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerOverrideRoleUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @Test
    @DisplayName("Given request for role mappings, When endpoint called, Then returns mappings list from database")
    void testGetMoodleRoleMappings() throws Exception {
        when(jdbcTemplate.queryForList("SELECT id, moodle_role_pattern, internal_role FROM moodle_role_mappings ORDER BY id ASC"))
                .thenReturn(List.of(
                        Map.of("id", 1L, "moodle_role_pattern", "администратор", "internal_role", "ADMIN"),
                        Map.of("id", 2L, "moodle_role_pattern", "исследователь", "internal_role", "RESEARCHER")
                ));

        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings", hasSize(2)))
                .andExpect(jsonPath("$.mappings[0].moodle_role_pattern", is("администратор")))
                .andExpect(jsonPath("$.mappings[0].internal_role", is("ADMIN")));
    }

    @Test
    @DisplayName("Given valid userId and role, When override-role called, Then updates user role atomically and returns success")
    void testOverrideMoodleRole_ByUserId_Success() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setUsername("test_user");
        user.setRole("USER");

        when(userService.findByUsernameOrEmail("101")).thenReturn(Optional.of(user));
        when(userService.updateRoleAtomically(101L, "USER", "ADMIN")).thenReturn(1);

        String requestBody = "{\"userId\": 101, \"role\": \"ADMIN\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user_id", is(101)))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        verify(userService).updateRoleAtomically(101L, "USER", "ADMIN");
    }

    @Test
    @DisplayName("Given valid username and role, When override-role called, Then updates user role atomically and returns success")
    void testOverrideMoodleRole_ByUsername_Success() throws Exception {
        User user = new User();
        user.setId(202L);
        user.setUsername("moodle_admin");
        user.setRole("RESEARCHER");

        when(userService.findByUsernameOrEmail("moodle_admin")).thenReturn(Optional.of(user));
        when(userService.updateRoleAtomically(202L, "RESEARCHER", "EPIDEMIOLOGIST")).thenReturn(1);

        String requestBody = "{\"username\": \"moodle_admin\", \"role\": \"EPIDEMIOLOGIST\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user_id", is(202)))
                .andExpect(jsonPath("$.role", is("EPIDEMIOLOGIST")));

        verify(userService).updateRoleAtomically(202L, "RESEARCHER", "EPIDEMIOLOGIST");
    }

    @Test
    @DisplayName("Given missing role, When override-role called, Then returns 400 Bad Request")
    void testOverrideMoodleRole_MissingRole_ReturnsBadRequest() throws Exception {
        String requestBody = "{\"userId\": 101}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("Given non-existent user, When override-role called, Then returns 400 Bad Request")
    void testOverrideMoodleRole_UserNotFound_ReturnsBadRequest() throws Exception {
        when(userService.findByUsernameOrEmail("unknown_user")).thenReturn(Optional.empty());

        String requestBody = "{\"username\": \"unknown_user\", \"role\": \"ADMIN\"}";

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("USER_NOT_FOUND")));
    }
}
