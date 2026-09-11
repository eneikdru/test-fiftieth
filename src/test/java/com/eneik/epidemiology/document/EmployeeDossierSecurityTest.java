package com.eneik.epidemiology.document;

import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.security.JwtAuthenticationFilter;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.security.SecurityConfig;
import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeDossierController.class, EmployeeDossierAnalyticsController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class EmployeeDossierSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeDocumentRepository employeeDocumentRepository;

    @MockBean
    private DossierReportRepository dossierReportRepository;

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

    private void configureMockToken(String token, String username, String role) {
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        Mockito.when(jwtTokenProvider.getRole(token)).thenReturn(role);
        Mockito.when(userService.resolveRoleByUsername(username)).thenReturn(java.util.Optional.of(role));
    }

    @Test
    @DisplayName("Given employee dossier endpoints, When accessed by an authorized user, Then no 401 errors are returned")
    void testDossierEndpoints_AuthorizedUser_No401Errors() throws Exception {
        String token = "valid_dossier_user_token";
        configureMockToken(token, "dossier_user", "USER");

        User dossierUser = new User();
        dossierUser.setUsername("dossier_user");
        dossierUser.setRole("USER");
        Mockito.when(userRepository.findByUsername("dossier_user")).thenReturn(java.util.Optional.of(dossierUser));

        Mockito.when(employeeDocumentRepository.searchEmployeeDocumentsSecure(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                Mockito.eq(false), isNull(), Mockito.eq(Collections.emptyList()), any(Pageable.class)
        )).thenReturn(new PageImpl<>(Collections.emptyList()));

        Mockito.when(dossierReportRepository.searchReportsSecure(
                isNull(), Mockito.eq(false), isNull(), Mockito.eq(Collections.emptyList()), any(Pageable.class)
        )).thenReturn(new PageImpl<>(Collections.emptyList()));

        Mockito.when(employeeDocumentRepository.searchEmployeeDocuments(
                Mockito.eq("EMP-100"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(Collections.emptyList()));

        Mockito.when(dossierReportRepository.save(any(DossierReport.class)))
                .thenAnswer(invocation -> {
                    DossierReport report = invocation.getArgument(0);
                    report.setId(100L);
                    return report;
                });
        Mockito.when(dossierReportRepository.updateStatus(any(), Mockito.eq("PENDING"), Mockito.eq("COMPLETED"))).thenReturn(1);

        // 1. GET /api/v1/dossier/documents
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. GET /api/v1/dossier/reports
        mockMvc.perform(get("/api/v1/dossier/reports")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. POST /api/v1/dossier/analytics/export
        mockMvc.perform(post("/api/v1/dossier/analytics/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("employee_id", "EMP-100"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Given employee dossier endpoints, When accessed without authentication, Then returns 401 Unauthorized")
    void testDossierEndpoints_Unauthenticated_Returns401Unauthorized() throws Exception {
        // 1. GET /api/v1/dossier/documents
        mockMvc.perform(get("/api/v1/dossier/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));

        // 2. GET /api/v1/dossier/reports
        mockMvc.perform(get("/api/v1/dossier/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));

        // 3. POST /api/v1/dossier/analytics/export
        mockMvc.perform(post("/api/v1/dossier/analytics/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("employee_id", "EMP-100"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Given the agent logic, When falsifiability checks are tested, Then missing database user returns 401 Unauthorized")
    void testDossierEndpoints_MissingDatabaseUser_Returns401Unauthorized() throws Exception {
        String token = "valid_but_deleted_user_token";
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(token)).thenReturn("deleted_user");
        Mockito.when(jwtTokenProvider.getRole(token)).thenReturn("USER");
        Mockito.when(userService.resolveRoleByUsername("deleted_user")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/dossier/documents")
                        .header("Authorization", "Bearer " + token))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
