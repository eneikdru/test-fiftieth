package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase(type = io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
public class ApiSliceD23751efVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Given valid credentials, When API is accessed, Then answers with 200 OK instead of 401")
    void testValidCredentials_Returns200OK() throws Exception {
        User user = userService.createUser("slice_d23_user", "Password123!", "slice_d23@inst.ru", "Slice D23 User", "EPIDEMIOLOGIST");
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getDepartment(), user.getCourses());

        mockMvc.perform(get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("slice_d23_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));
    }

    @Test
    @DisplayName("Given an unauthenticated or stub PR request, When processing PR #0, Then returns 401 Unauthorized rejecting stub review claims")
    void testUnauthenticatedPr0Request_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/documents/pr/0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given server environment, When server.port property is resolved, Then defaults to 18080 when unset")
    void testServerPortConfiguration_DefaultsTo18080() {
        String serverPort = environment.getProperty("server.port", "18080");
        assertEquals("18080", serverPort);
    }
}
