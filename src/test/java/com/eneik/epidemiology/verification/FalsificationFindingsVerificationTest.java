package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.auth.AuthController;
import com.eneik.epidemiology.auth.PasswordRecoveryService;
import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.document.EmployeeDossierController;
import com.eneik.epidemiology.security.JwtAuthenticationFilter;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.security.SecurityConfig;
import com.eneik.epidemiology.telemetry.TelemetryService;
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

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeDossierController.class, AuthController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class FalsificationFindingsVerificationTest {

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
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    private void configureMockToken(String token, String username, String role) {
        Mockito.when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(token)).thenReturn(username);
        Mockito.when(jwtTokenProvider.getRole(token)).thenReturn(role);
    }

    @Test
    @DisplayName("Finding 1: Unauthenticated request to employee dossier endpoint returns 401 UNAUTHORIZED")
    void testFinding1_EmployeeDossierUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Finding 1: Authenticated request to employee dossier endpoint returns 200 OK")
    void testFinding1_EmployeeDossierAuthenticatedReturns200() throws Exception {
        String token = "valid_user_token";
        configureMockToken(token, "test_user", "USER");

        Mockito.when(employeeDocumentRepository.searchEmployeeDocumentsSecure(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                Mockito.eq(false), isNull(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/dossier/documents")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Finding 1: Public access to Moodle SSO config returns 200 OK")
    void testFinding1_MoodleSsoConfigPublicAccessReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_url").exists());
    }

    @Test
    @DisplayName("Finding 2 & 4: Verify strict refusal criteria without hallucinations via CodeReviewGuardianValidator")
    void testFinding2And4_StrictRefusalCriteriaWithoutHallucinations() {
        CodeReviewGuardianValidator validator = new CodeReviewGuardianValidator();

        Set<String> actualDiff = Set.of("src/main/java/com/eneik/epidemiology/document/EmployeeDocument.java");

        // Scenario 1: Review evaluates a hallucinated file (ProtocolController) not present in diff -> REJECT
        Set<String> hallucinatedEvaluation = Set.of(
                "src/main/java/com/eneik/epidemiology/document/EmployeeDocument.java",
                "src/main/java/com/eneik/epidemiology/document/ProtocolController.java"
        );
        CodeReviewGuardianValidator.ReviewResult hallucinationResult =
                validator.evaluateReview(actualDiff, hallucinatedEvaluation, false);

        assertEquals(CodeReviewGuardianValidator.ReviewVerdict.REJECT, hallucinationResult.verdict(),
                "Verdict must be REJECT when review evaluates files not in diff");
        assertFalse(hallucinationResult.invalidEvaluatedFiles().isEmpty(),
                "Invalid evaluated files must be reported");
        assertTrue(hallucinationResult.invalidEvaluatedFiles().contains("src/main/java/com/eneik/epidemiology/document/ProtocolController.java"));

        // Scenario 2: PR contains out-of-scope modifications -> REJECT
        CodeReviewGuardianValidator.ReviewResult outOfScopeResult =
                validator.evaluateReview(actualDiff, actualDiff, true);

        assertEquals(CodeReviewGuardianValidator.ReviewVerdict.REJECT, outOfScopeResult.verdict(),
                "Verdict must be REJECT when PR has out-of-scope changes");

        // Scenario 3: Valid PR without out-of-scope changes or hallucinations -> APPROVE
        CodeReviewGuardianValidator.ReviewResult validResult =
                validator.evaluateReview(actualDiff, actualDiff, false);

        assertEquals(CodeReviewGuardianValidator.ReviewVerdict.APPROVE, validResult.verdict(),
                "Verdict must be APPROVE for grounded and scoped review");
        assertTrue(validResult.invalidEvaluatedFiles().isEmpty());
    }

    @Test
    @DisplayName("Finding 3: Verify execution permissions on backup and restore scripts")
    void testFinding3_BackupScriptExecutionPermissions() {
        File backupScript = new File("scripts/backup.sh");
        File restoreScript = new File("scripts/restore.sh");

        assertTrue(backupScript.exists(), "scripts/backup.sh must exist");
        assertTrue(restoreScript.exists(), "scripts/restore.sh must exist");

        assertTrue(backupScript.canExecute(), "scripts/backup.sh must have executable permission (+x)");
        assertTrue(restoreScript.canExecute(), "scripts/restore.sh must have executable permission (+x)");
    }
}
