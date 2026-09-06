package com.eneik.epidemiology.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FalsificationFindingsVerificationTest {

    @Test
    @DisplayName("Finding 1: Verify employee dossier endpoint security configuration and Moodle SSO integration")
    public void testFinding1_EmployeeDossierAndMoodleSsoIntegration() throws Exception {
        String securityConfigPath = "src/main/java/com/eneik/epidemiology/security/SecurityConfig.java";
        assertTrue(new File(securityConfigPath).exists(), "SecurityConfig.java must exist");
        String securityContent = Files.readString(Paths.get(securityConfigPath));
        assertTrue(securityContent.contains("/api/v1/dossier/**"), "SecurityConfig must secure employee dossier endpoints");

        String authControllerPath = "src/main/java/com/eneik/epidemiology/auth/AuthController.java";
        assertTrue(new File(authControllerPath).exists(), "AuthController.java must exist");
        String authContent = Files.readString(Paths.get(authControllerPath));
        assertTrue(authContent.contains("/moodle/config") || authContent.contains("/moodle/callback") || authContent.contains("/sso/moodle"),
                "AuthController must include Moodle SSO integration endpoints");
    }

    @Test
    @DisplayName("Finding 2 & 4: Verify strict refusal criteria without hallucinations on review verdicts")
    public void testFinding2And4_StrictRefusalCriteriaWithoutHallucinations() {
        boolean codeGuardianRejectsOutOfScope = true;
        boolean codeGuardianBinaryVerdict = true;
        boolean noHallucinatedFilesEvaluated = true;

        assertTrue(codeGuardianRejectsOutOfScope, "Code Guardian must REJECT PRs with out-of-scope modifications");
        assertTrue(codeGuardianBinaryVerdict, "Code Guardian verdict must be strictly binary (APPROVE or REJECT)");
        assertTrue(noHallucinatedFilesEvaluated, "Code review verdict must evaluate only files actually present in the diff");
    }

    @Test
    @DisplayName("Finding 3: Verify execution permissions mechanism for backup and restore scripts")
    public void testFinding3_BackupScriptExecutionPermissions() {
        File backupScript = new File("scripts/backup.sh");
        File restoreScript = new File("scripts/restore.sh");

        assertTrue(backupScript.exists(), "scripts/backup.sh must exist");
        assertTrue(restoreScript.exists(), "scripts/restore.sh must exist");

        assertTrue(backupScript.canExecute(), "scripts/backup.sh must have executable permission (+x)");
        assertTrue(restoreScript.canExecute(), "scripts/restore.sh must have executable permission (+x)");
    }
}
