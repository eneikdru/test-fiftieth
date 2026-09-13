package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class AuthBoundaryValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        if (userService.findByUsername("valid_auth_user").isEmpty()) {
            userService.createUser("valid_auth_user", "ValidSecurePassword123!", "valid_auth@epidemiology-inst.ru", "Тестовый Исследователь", "RESEARCHER");
        }
    }

    @Test
    @DisplayName("Given valid credentials, When authenticating, Then server grants access token and allows access to protected data")
    void testAuthenticationBoundary_ValidCredentials_GrantsAccessToProtectedData() throws Exception {
        String loginPayload = "{" +
                "\"username\":\"valid_auth_user\"," +
                "\"password\":\"ValidSecurePassword123!\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("valid_auth_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        String accessToken = jsonNode.get("access_token").asText();

        // Verify protected endpoint access with token
        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("valid_auth_user")));
    }

    @Test
    @DisplayName("Given invalid credentials, When authenticating, Then server explicitly denies access with 401 Unauthorized")
    void testAuthenticationBoundary_InvalidCredentials_ExplicitlyDenied() throws Exception {
        String invalidLoginPayload = "{" +
                "\"username\":\"valid_auth_user\"," +
                "\"password\":\"WrongPassword123!\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLoginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Given valid registration request, When authenticating endpoint called, Then returns 201 Created and user information")
    void testAuthenticationBoundary_ValidRegistration_Returns201Created() throws Exception {
        String registerPayload = "{" +
                "\"username\":\"new_boundary_user\"," +
                "\"password\":\"StrongPass123!\"," +
                "\"email\":\"new_boundary@epidemiology-inst.ru\"," +
                "\"full_name\":\"Новый Пользователь\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("new_boundary_user")));
    }

    @Test
    @DisplayName("Given missing request parameters, When authenticating or registering, Then returns 400 Bad Request")
    void testAuthenticationBoundary_MissingParameters_Returns400BadRequest() throws Exception {
        String invalidPayload = "{\"username\":\"incomplete_user\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_REQUEST")));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("Given missing or unauthenticated request, When accessing protected endpoints, Then returns 401 Unauthorized")
    void testAuthenticationBoundary_UnauthenticatedRequest_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Given a non-admin authenticated user, When accessing admin-restricted endpoint, Then returns 403 Forbidden")
    void testAuthenticationBoundary_InsufficientPermissions_Returns403Forbidden() throws Exception {
        String loginPayload = "{" +
                "\"username\":\"valid_auth_user\"," +
                "\"password\":\"ValidSecurePassword123!\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        String accessToken = jsonNode.get("access_token").asText();

        mockMvc.perform(get("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }
}
