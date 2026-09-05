package com.eneik.epidemiology.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDocumentRestoreNegativeVerificationTest {

    @Test
    @DisplayName("Given missing database backup file, When restore operation runs, Then it fails with an error")
    void testRestoreWorkflow_MissingDbBackup(@TempDir Path tempDir) throws Exception {
        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);

        ProcessBuilder restorePb = new ProcessBuilder("bash", "scripts/restore.sh");
        Map<String, String> restoreEnv = restorePb.environment();
        restoreEnv.put("BACKUP_DIR", backupDir.toAbsolutePath().toString());
        restoreEnv.put("DB_BACKUP_FILE", "missing_db.sql.gz");
        restoreEnv.put("UPLOADS_BACKUP_FILE", "missing_uploads.tar.gz");

        Process restoreProc = restorePb.start();
        int exitCode = restoreProc.waitFor();
        String errorOutput = new String(restoreProc.getErrorStream().readAllBytes());

        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorOutput).contains("ERROR: Database backup archive 'missing_db.sql.gz' not found!");
    }

    @Test
    @DisplayName("Given missing uploads backup file, When restore operation runs, Then it fails with an error")
    void testRestoreWorkflow_MissingUploadsBackup(@TempDir Path tempDir) throws Exception {
        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);

        Path dummyDbBackup = backupDir.resolve("dummy_db.sql.gz");
        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(dummyDbBackup.toFile()))) {
            // empty content
        }

        ProcessBuilder restorePb = new ProcessBuilder("bash", "scripts/restore.sh");
        Map<String, String> restoreEnv = restorePb.environment();
        restoreEnv.put("BACKUP_DIR", backupDir.toAbsolutePath().toString());
        restoreEnv.put("DB_BACKUP_FILE", dummyDbBackup.toAbsolutePath().toString());
        restoreEnv.put("UPLOADS_BACKUP_FILE", "missing_uploads.tar.gz");
        restoreEnv.put("SQLITE_DB_PATH", tempDir.resolve("test.db").toAbsolutePath().toString());

        Process restoreProc = restorePb.start();
        int exitCode = restoreProc.waitFor();
        String errorOutput = new String(restoreProc.getErrorStream().readAllBytes());

        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorOutput).contains("ERROR: Uploads backup archive 'missing_uploads.tar.gz' not found!");
    }
}
