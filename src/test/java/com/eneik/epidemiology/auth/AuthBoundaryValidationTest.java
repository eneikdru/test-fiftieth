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
}
