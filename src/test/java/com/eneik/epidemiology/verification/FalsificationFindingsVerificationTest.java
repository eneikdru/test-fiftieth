package com.eneik.epidemiology.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FalsificationFindingsVerificationTest {

    @Test
    @DisplayName("Finding 1: Verify employee dossier endpoint security configuration and Moodle SSO integration presence")
    public void testFinding1_EmployeeDossierAndMoodleSsoIntegration() throws Exception {
        String securityConfigPath = "src/main/java/com/eneik/epidemiology/security/SecurityConfig.java";
        assertTrue(new File(securityConfigPath).exists(), "SecurityConfig.java must exist");
        String securityContent = Files.readString(Paths.get(securityConfigPath));
        assertTrue(securityContent.contains("/api/v1/dossier/**"), "SecurityConfig must secure employee dossier endpoints");

        String authControllerPath = "src/main/java/com/eneik/epidemiology/auth/AuthController.java";
        assertTrue(new File(authControllerPath).exists(), "AuthController.java must exist");
        String authContent = Files.readString(Paths.get(authControllerPath));
        assertTrue(authContent.contains("/moodle/config") && authContent.contains("/moodle/callback") && authContent.contains("/sso/moodle"),
                "AuthController must include all required Moodle SSO endpoints (/moodle/config, /moodle/callback, /sso/moodle)");
    }

    @Test
    @DisplayName("Finding 2 & 4: Verify strict refusal criteria without hallucinations via CodeReviewGuardianValidator")
    public void testFinding2And4_StrictRefusalCriteriaWithoutHallucinations() {
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
    @DisplayName("Finding 3: Verify native execution permissions for backup and restore scripts")
    public void testFinding3_BackupScriptExecutionPermissions() throws Exception {
        File backupScript = new File("scripts/backup.sh");
        File restoreScript = new File("scripts/restore.sh");

        assertTrue(backupScript.exists(), "scripts/backup.sh must exist");
        assertTrue(restoreScript.exists(), "scripts/restore.sh must exist");

        assertTrue(backupScript.canExecute(), "scripts/backup.sh must have executable permission (+x)");
        assertTrue(restoreScript.canExecute(), "scripts/restore.sh must have executable permission (+x)");

        // Test native execution of backup script using system ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder("scripts/backup.sh");
        pb.environment().put("ALLOW_MOCK_BACKUP", "1");
        pb.environment().put("BACKUP_DIR", "./tmp_verification_backup");
        pb.environment().put("UPLOADS_DIR", "./tmp_verification_uploads");

        Process process = pb.start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "backup.sh must execute natively without permission errors");

        // Cleanup temporary directory created during process test
        File tmpDir = new File("./tmp_verification_backup");
        if (tmpDir.exists()) {
            for (File file : tmpDir.listFiles()) {
                file.delete();
            }
            tmpDir.delete();
        }
    }
}
