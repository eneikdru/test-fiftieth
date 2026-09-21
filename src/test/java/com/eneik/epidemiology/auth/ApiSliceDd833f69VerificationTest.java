package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.strain.Strain;
import com.eneik.epidemiology.strain.StrainRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private StrainRepository strainRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(authController.getRestTemplate()).build();
    }

    @Test
    @DisplayName("Given an unauthenticated request to employee dossier search, document listing, or report endpoints, When invoked without valid credentials, Then HTTP 401 Unauthorized is returned")
    void testUnauthenticatedRequests_Return401Unauthorized() throws Exception {
        // Dossier search endpoint
        mockMvc.perform(get("/api/v1/dossier/documents").param("employee_id", "EMP-SLICE-01"))
                .andExpect(status().isUnauthorized());

        // Document listing
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized());

        // Document full-text search
        mockMvc.perform(get("/api/v1/documents/search").param("q", "epidemiology"))
                .andExpect(status().isUnauthorized());

        // Dossier reports list
        mockMvc.perform(get("/api/v1/dossier/reports"))
                .andExpect(status().isUnauthorized());

        // Report generation
        Map<String, Object> request = Map.of("employee_id", "EMP-SLICE-01", "template_type", "SUMMARY_STANDARD");
        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Strains list
        mockMvc.perform(get("/api/v1/strains"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given a request to report generation using real JWT authentication without @WithMockUser, When executed, Then it returns 201 Created")
    void testReportGenerationEndpoint_RealJwtAuth_Returns201Created() throws Exception {
        User user = new User();
        user.setUsername("jwt_report_user");
        user.setRole("USER");
        user.setDepartment("Эпидемиология");
        user.setCourses("EPID-101");
        user.setEmail("jwt_report@inst.ru");
        user.setPasswordHash("hash");
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);

        EmployeeDocument doc = new EmployeeDocument(
                "EMP-SLICE-01",
                "Иванов",
                "REPORT",
                "Отчет по инфекционной заболеваемости",
                LocalDate.of(2026, 9, 1),
                "EPIDEMIOLOGY"
        );
        doc.setAccessDepartment("Эпидемиология");
        doc.setAccessCourse("EPID-101");
        employeeDocumentRepository.save(doc);

        String realJwtToken = jwtTokenProvider.generateToken("jwt_report_user", "USER", "Эпидемиология", "EPID-101");

        Map<String, Object> request = Map.of(
                "employee_id", "EMP-SLICE-01",
                "template_type", "SUMMARY_STANDARD"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + realJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.employee_id", is("EMP-SLICE-01")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("Given an authenticated user, When requesting closed strains or reports, Then access is automatically restricted based on Moodle departments and courses")
    void testMoodleDepartmentAndCourseAccessControl_ForStrainsAndReports() throws Exception {
        // User 1: Department = "Эпидемиология", Course = "EPID-101"
        User epidUser = new User();
        epidUser.setUsername("epid_user_access");
        epidUser.setRole("USER");
        epidUser.setDepartment("Эпидемиология");
        epidUser.setCourses("EPID-101");
        epidUser.setEmail("epid_access@inst.ru");
        epidUser.setPasswordHash("hash");
        epidUser.setCreatedAt(OffsetDateTime.now());
        userRepository.save(epidUser);

        // User 2: Department = "Вирусология", Course = "VIRO-202"
        User viroUser = new User();
        viroUser.setUsername("viro_user_access");
        viroUser.setRole("USER");
        viroUser.setDepartment("Вирусология");
        viroUser.setCourses("VIRO-202");
        viroUser.setEmail("viro_access@inst.ru");
        viroUser.setPasswordHash("hash");
        viroUser.setCreatedAt(OffsetDateTime.now());
        userRepository.save(viroUser);

        // Create Closed Strain 1 for Epidemiology
        Strain epidStrain = new Strain();
        epidStrain.setId(UUID.randomUUID());
        epidStrain.setName("Epidemiology Restricted Strain");
        epidStrain.setAccessDepartment("Эпидемиология");
        epidStrain.setAccessCourse("EPID-101");
        strainRepository.save(epidStrain);

        // Create Closed Dossier Report 1 for Epidemiology
        DossierReport epidReport = new DossierReport(
                "EMP-ACCESS-01",
                "FULL",
                "COMPLETED",
                "Epidemiology Summary Report",
                1,
                "/api/v1/dossier/reports/download"
        );
        epidReport.setAccessDepartment("Эпидемиология");
        epidReport.setAccessCourse("EPID-101");
        dossierReportRepository.save(epidReport);

        String epidToken = jwtTokenProvider.generateToken("epid_user_access", "USER", "Эпидемиология", "EPID-101");
        String viroToken = jwtTokenProvider.generateToken("viro_user_access", "USER", "Вирусология", "VIRO-202");

        // 1. Epid user can view their closed strain
        mockMvc.perform(get("/api/v1/strains/" + epidStrain.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + epidToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Epidemiology Restricted Strain")));

        // 2. Viro user gets 403 Forbidden when requesting the Epidemiology closed strain
        mockMvc.perform(get("/api/v1/strains/" + epidStrain.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viroToken))
                .andExpect(status().isForbidden());

        // 3. Epid user can view their report
        mockMvc.perform(get("/api/v1/dossier/reports/" + epidReport.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + epidToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_id", is("EMP-ACCESS-01")));

        // 4. Viro user gets 403 Forbidden when requesting the Epidemiology report
        mockMvc.perform(get("/api/v1/dossier/reports/" + epidReport.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viroToken))
                .andExpect(status().isForbidden());
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
