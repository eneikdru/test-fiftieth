package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class EpidemiologistRoleAndJwtAuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Given Moodle role 'epidemiologist', When LTI or SSO launch is processed, Then user is provisioned with EPIDEMIOLOGIST role")
    void testEpidemiologistRoleMapping_InLtiLaunch() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "epi_moodle_user")
                .param("full_name", "Эпидемиолог Иванов")
                .param("email", "epi_iv@epidemiology-inst.ru")
                .param("roles", "epidemiologist")
                .param("department", "Отдел эпидемиологии")
                .param("custom_courses", "EPID-501")
                .param("oauth_signature", "valid_lti_signature"))
                .andExpect(status().isFound());

        User user = userService.findByUsername("epi_moodle_user").orElseThrow();
        assertEquals("EPIDEMIOLOGIST", user.getRole());
        assertEquals("Отдел эпидемиологии", user.getDepartment());
        assertEquals("EPID-501", user.getCourses());
    }

    @Test
    @DisplayName("Given fallback SSO login, When executed, Then SecurityContext is NOT set during handler execution and JWT filter processes subsequent requests")
    void testStatelessFallbackAuth_DoesNotPolluteSecurityContext_AndJwtFilterProcessesToken() throws Exception {
        userService.createUser("fallback_epi_user", "ValidFallbackPass123!", "fallback_epi@inst.ru", "Эпидемиолог Петров", "EPIDEMIOLOGIST");

        String ssoBody = "{\"username\":\"fallback_epi_user\",\"moodle_token\":\"invalid_moodle_token\",\"fallback_password\":\"ValidFallbackPass123!\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andReturn();

        // SecurityContext should remain unpopulated in a stateless endpoint
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "SecurityContextHolder must remain unpopulated in stateless login endpoint thread");

        String responseContent = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseContent);
        String token = root.get("access_token").asText();

        // Verify that JwtAuthenticationFilter actually processes the token for subsequent requests
        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("fallback_epi_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));
    }
}
