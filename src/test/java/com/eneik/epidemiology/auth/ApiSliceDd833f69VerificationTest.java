package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase(type = io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
class ApiSliceDd833f69VerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(authController.getRestTemplate()).build();
    }

    @Test
    @WithMockUser(username = "report_user_slice", roles = "USER")
    @DisplayName("Given an authenticated request to report generation, When the endpoint is called, Then it returns 201 Created instead of 401 Unauthorized")
    void testReportGenerationEndpoint_AuthenticatedUser_Returns201Created() throws Exception {
        EmployeeDocument doc = new EmployeeDocument(
                "EMP-SLICE-01",
                "Иванов",
                "REPORT",
                "Отчет по инфекционной заболеваемости",
                LocalDate.of(2026, 9, 1),
                "EPIDEMIOLOGY"
        );
        employeeDocumentRepository.save(doc);

        Map<String, Object> request = Map.of(
                "employee_id", "EMP-SLICE-01",
                "template_type", "SUMMARY_STANDARD"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.employee_id", is("EMP-SLICE-01")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("Given a user logs in via Moodle SSO, When they authenticate successfully, Then their roles are synchronized")
    void testMoodleSsoLogin_SynchronizesUserRoles() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_epid_user\",\"role\":\"Эпидемиолог\",\"department\":\"Эпидемиология\",\"email\":\"epid@inst.ru\",\"full_name\":\"Иван Эпидемиологов\"}",
                        MediaType.APPLICATION_JSON
                ));

        Map<String, String> request = Map.of(
                "username", "moodle_epid_user",
                "moodle_token", "valid_moodle_token_slice"
        );

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_epid_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));

        User user = userRepository.findByUsername("moodle_epid_user").orElseThrow();
        assertEquals("EPIDEMIOLOGIST", user.getRole());
        mockServer.verify();
    }

    @Test
    @DisplayName("Given the external LMS is down, When a user logs in with fallback password, Then they can use the autonomous fallback")
    void testMoodleSso_AutonomousFallbackWhenLmsDown() throws Exception {
        User user = userService.createUser(
                "fallback_user_slice",
                "SecretPass123!",
                "fallback@inst.ru",
                "Петр Резервный",
                "USER"
        );

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        Map<String, String> request = Map.of(
                "username", "fallback_user_slice",
                "moodle_token", "invalid_or_unreachable_token",
                "fallback_password", "SecretPass123!"
        );

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("fallback_user_slice")));

        mockServer.verify();
    }
}
