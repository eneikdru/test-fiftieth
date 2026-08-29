package com.eneik.epidemiology.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDocumentRestoreVerificationTest {

    @Test
    @DisplayName("Given automated backup, When restore operation runs in fresh environment, Then employee documents and data integrity match time of backup")
    void testBackupRestoreWorkflow_AllEmployeeDocumentsRestored(@TempDir Path tempDir) throws Exception {
        Path originalUploads = tempDir.resolve("original_uploads");
        Path originalDbDir = tempDir.resolve("original_db");
        Path backupDir = tempDir.resolve("backups");
        Path restoredUploads = tempDir.resolve("restored_uploads");
        Path restoredDbDir = tempDir.resolve("restored_db");

        Files.createDirectories(originalUploads);
        Files.createDirectories(originalDbDir);
        Files.createDirectories(backupDir);
        Files.createDirectories(restoredUploads);
        Files.createDirectories(restoredDbDir);

        Path originalDb = originalDbDir.resolve("epidemiology.db");
        Path restoredDb = restoredDbDir.resolve("restored_epidemiology.db");

        // 1. Prepare initial dataset at time of backup (Employee documents + SQLite DB records)
        String doc1Name = "employee_virology_pub_2026.pdf";
        String doc1Content = "Scientific publication on Virology by Dr. Ivanov 2026";
        String doc2Name = "employee_bacteriology_order.docx";
        String doc2Content = "Official executive order on Bacteriology laboratory 2026";

        Files.writeString(originalUploads.resolve(doc1Name), doc1Content, StandardCharsets.UTF_8);
        Files.writeString(originalUploads.resolve(doc2Name), doc2Content, StandardCharsets.UTF_8);

        // Populate database schema & sample employee document records using sqlite3 process
        ProcessBuilder initDbBuilder = new ProcessBuilder(
                "sqlite3", originalDb.toAbsolutePath().toString()
        );
        Process initDbProc = initDbBuilder.start();
        String initSql = "CREATE TABLE employee_documents (" +
                "id INTEGER PRIMARY KEY, " +
                "employee_name TEXT NOT NULL, " +
                "scientific_direction TEXT NOT NULL, " +
                "document_title TEXT NOT NULL, " +
                "document_type TEXT NOT NULL" +
                ");\n" +
                "INSERT INTO employee_documents VALUES (1, 'Dr. Ivanov', 'Virology', 'Virology Research Paper', 'PUBLICATION');\n" +
                "INSERT INTO employee_documents VALUES (2, 'Dr. Petrov', 'Bacteriology', 'Bacteriology Safety Order', 'ORDER');\n";
        initDbProc.getOutputStream().write(initSql.getBytes(StandardCharsets.UTF_8));
        initDbProc.getOutputStream().flush();
        initDbProc.getOutputStream().close();
        assertThat(initDbProc.waitFor()).isEqualTo(0);

        // 2. Trigger Backup process (scripts/backup.sh)
        ProcessBuilder backupPb = new ProcessBuilder("bash", "scripts/backup.sh");
        Map<String, String> backupEnv = backupPb.environment();
        backupEnv.put("SQLITE_DB_PATH", originalDb.toAbsolutePath().toString());
        backupEnv.put("UPLOADS_DIR", originalUploads.toAbsolutePath().toString());
        backupEnv.put("BACKUP_DIR", backupDir.toAbsolutePath().toString());
        backupEnv.put("BACKUP_RETENTION_DAYS", "7");
        backupEnv.put("OVERRIDE_TIMESTAMP", "20260825_191623");

        Process backupProc = backupPb.start();
        assertThat(backupProc.waitFor()).isEqualTo(0);

        Path dbBackupFile = backupDir.resolve("db_epidemiology_db_20260825_191623.sql.gz");
        Path uploadsBackupFile = backupDir.resolve("uploads_20260825_191623.tar.gz");
        assertThat(Files.exists(dbBackupFile)).isTrue();
        assertThat(Files.exists(uploadsBackupFile)).isTrue();

        // 3. Trigger Restore process into clean target environment (scripts/restore.sh)
        ProcessBuilder restorePb = new ProcessBuilder("bash", "scripts/restore.sh");
        Map<String, String> restoreEnv = restorePb.environment();
        restoreEnv.put("SQLITE_DB_PATH", restoredDb.toAbsolutePath().toString());
        restoreEnv.put("UPLOADS_DIR", restoredUploads.toRealPath().toString());
        restoreEnv.put("BACKUP_DIR", backupDir.toRealPath().toString());
        restoreEnv.put("DB_BACKUP_FILE", dbBackupFile.toRealPath().toString());
        restoreEnv.put("UPLOADS_BACKUP_FILE", uploadsBackupFile.toRealPath().toString());

        Process restoreProc = restorePb.start();
        assertThat(restoreProc.waitFor()).isEqualTo(0);

        // 4. Assert all employee documents and database records are successfully restored and accessible
        assertThat(Files.exists(restoredUploads.resolve(doc1Name))).isTrue();
        assertThat(Files.exists(restoredUploads.resolve(doc2Name))).isTrue();
        assertThat(Files.readString(restoredUploads.resolve(doc1Name), StandardCharsets.UTF_8)).isEqualTo(doc1Content);
        assertThat(Files.readString(restoredUploads.resolve(doc2Name), StandardCharsets.UTF_8)).isEqualTo(doc2Content);

        // Execute validation query against restored database
        ProcessBuilder queryPb = new ProcessBuilder("sqlite3", restoredDb.toAbsolutePath().toString(),
                "SELECT employee_name, scientific_direction, document_title FROM employee_documents ORDER BY id ASC;");
        Process queryProc = queryPb.start();
        String queryOutput = new String(queryProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertThat(queryProc.waitFor()).isEqualTo(0);

        List<String> lines = List.of(queryOutput.split("\n"));
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).isEqualTo("Dr. Ivanov|Virology|Virology Research Paper");
        assertThat(lines.get(1)).isEqualTo("Dr. Petrov|Bacteriology|Bacteriology Safety Order");
    }
}
