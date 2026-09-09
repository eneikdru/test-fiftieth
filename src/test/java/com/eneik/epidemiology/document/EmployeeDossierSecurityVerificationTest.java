package com.eneik.epidemiology.document;

import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.security.JwtAuthenticationFilter;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.security.SecurityConfig;
import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeDossierController.class, EmployeeDossierAnalyticsController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class EmployeeDossierSecurityVerificationTest {

    @Autowired
    private MockMvc mockMvc;

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

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setRole(role);
        mockUser.setDepartment("Epidemiology");
        Mockito.when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        Mockito.when(userService.resolveRoleByUsername(username)).thenReturn(Optional.of(role));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "EPIDEMIOLOGIST", "RESEARCHER"})
    @DisplayName("Given employee dossier endpoints, When accessed by authorized user, Then no 401 errors are returned")
    void testEmployeeDossierEndpoints_AuthorizedUsers_No401Errors(String role) throws Exception {
        String token = "valid_" + role.toLowerCase() + "_token";
        String username = role.toLowerCase() + "_user";
        configureMockToken(token, username, role);

        Mockito.when(employeeDocumentRepository.searchEmployeeDocumentsSecure(
                any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Mockito.when(dossierReportRepository.searchReportsSecure(
                any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // 1. Employee Dossier Documents endpoint
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Employee Dossier Reports endpoint
        mockMvc.perform(get("/api/v1/dossier/reports")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Employee Dossier Analytics Documents endpoint
        mockMvc.perform(get("/api/v1/dossier/analytics/documents")
                        .param("employee_id", "EMP-101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 4. Employee Dossier Analytics Metrics endpoint
        Mockito.when(employeeDocumentRepository.searchEmployeeDocuments(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/dossier/analytics/metrics")
                        .param("employee_id", "EMP-101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given employee dossier endpoints, When accessed without authentication, Then 401 Unauthorized is returned")
    void testEmployeeDossierEndpoints_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/dossier/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }
}
