package com.eneik.epidemiology.security;

import com.eneik.epidemiology.auth.AuthController;
import com.eneik.epidemiology.auth.PasswordRecoveryService;
import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.document.*;
import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        EmployeeDossierController.class,
        EmployeeDossierAnalyticsController.class,
        ProtocolController.class,
        DocumentController.class,
        AuthController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class RoleBasedAccessControlSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeDocumentRepository employeeDocumentRepository;

    @MockBean
    private DossierReportRepository dossierReportRepository;

    @MockBean
    private DocumentRepository documentRepository;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserService userService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PasswordRecoveryService passwordRecoveryService;

    private void configureMockToken(String token, String username, String role) {
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        Mockito.when(jwtTokenProvider.getRole(token)).thenReturn(role);
    }

    @Test
    @DisplayName("Given valid USER role token, When accessing dossier endpoints, Then returns 200 OK")
    void testDossierAccess_UserRole_Granted() throws Exception {
        String token = "user_token";
        configureMockToken(token, "user1", "USER");

        User mockUser = new User();
        mockUser.setUsername("user1");
        mockUser.setRole("USER");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(java.util.Optional.of(mockUser));

        Mockito.when(employeeDocumentRepository.searchEmployeeDocumentsSecure(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                Mockito.eq(false), isNull(), Mockito.eq(Collections.emptyList()), any(Pageable.class)
        )).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/dossier/documents")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given valid USER role token, When generating dossier report, Then returns 201 Created")
    void testDossierReportGeneration_UserRole_Returns201Created() throws Exception {
        String token = "user_token";
        configureMockToken(token, "user1", "USER");

        User mockUser = new User();
        mockUser.setUsername("user1");
        mockUser.setRole("USER");
        Mockito.when(userRepository.findByUsername("user1")).thenReturn(java.util.Optional.of(mockUser));

        DossierReport report = new DossierReport("EMP-100", "SUMMARY", "PENDING", null, 0, null);
        report.setId(100L);
        Mockito.when(dossierReportRepository.save(any(DossierReport.class))).thenReturn(report);
        Mockito.when(dossierReportRepository.updateStatus(any(), Mockito.eq("PENDING"), Mockito.eq("COMPLETED"))).thenReturn(1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/dossier/reports")
                .header("Authorization", "Bearer " + token)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"employee_id\":\"EMP-100\",\"template_type\":\"SUMMARY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("Given valid RESEARCHER role token, When accessing protocols endpoint, Then returns 200 OK")
    void testProtocolsAccess_ResearcherRole_Granted() throws Exception {
        String token = "researcher_token";
        configureMockToken(token, "researcher1", "RESEARCHER");

        Mockito.when(documentRepository.fullTextSearch(isNull(), Mockito.eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given valid USER role token, When attempting to access protocols endpoint, Then returns 403 Forbidden")
    void testProtocolsAccess_UserRole_Forbidden() throws Exception {
        String token = "user_token";
        configureMockToken(token, "user1", "USER");

        mockMvc.perform(get("/api/v1/protocols")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Given valid ADMIN role token, When deleting document, Then request passes role security")
    void testDocumentDelete_AdminRole_Allowed() throws Exception {
        String token = "admin_token";
        configureMockToken(token, "admin1", "ADMIN");

        mockMvc.perform(delete("/api/v1/documents/42")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given valid RESEARCHER role token, When attempting to delete document, Then returns 403 Forbidden")
    void testDocumentDelete_ResearcherRole_Forbidden() throws Exception {
        String token = "researcher_token";
        configureMockToken(token, "researcher1", "RESEARCHER");

        mockMvc.perform(delete("/api/v1/documents/42")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Given valid ADMIN role token, When accessing Moodle role override endpoint, Then returns 200 OK")
    void testMoodleRoleOverride_AdminRole_Granted() throws Exception {
        String token = "admin_token";
        configureMockToken(token, "admin1", "ADMIN");

        Mockito.when(jdbcTemplate.queryForList(any(String.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given valid RESEARCHER role token, When attempting to access Moodle role override endpoint, Then returns 403 Forbidden")
    void testMoodleRoleOverride_ResearcherRole_Forbidden() throws Exception {
        String token = "researcher_token";
        configureMockToken(token, "researcher1", "RESEARCHER");

        mockMvc.perform(get("/api/v1/auth/moodle/override-role")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Given unauthenticated request to protected endpoints, Then returns 401 Unauthorized")
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/protocols"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));

        mockMvc.perform(delete("/api/v1/documents/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
