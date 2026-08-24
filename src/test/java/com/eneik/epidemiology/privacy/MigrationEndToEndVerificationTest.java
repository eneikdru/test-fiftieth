package com.eneik.epidemiology.privacy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Transactional
class MigrationEndToEndVerificationTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    private void runScript(Connection conn, String path) {
        ScriptUtils.executeSqlScript(
            conn,
            new EncodedResource(new ClassPathResource(path)),
            false,
            false,
            ScriptUtils.DEFAULT_COMMENT_PREFIX,
            ScriptUtils.EOF_STATEMENT_SEPARATOR,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
        );
    }

    @Test
    @DisplayName("Given full migration chain runs, When end-to-end check executes, Then test asserts terminal ESCALATED state for recovery subjects")
    void testFullMigrationChainAssertsTerminalEscalatedState() {
        String subject1 = "fd6672c6-02c4-455e-a4d9-91e4ae9d308c";
        String subject2 = "765d2ab0-1b55-4701-babd-af5247442de5";

        // Seed initial pending/processing rows as would exist before unblocking migrations
        DataExportJob exp1 = new DataExportJob();
        exp1.setRequestId("e2e-exp-fd6672");
        exp1.setSubjectId(subject1);
        exp1.setStatus("PENDING");
        exp1.setRequestedFormat("ZIP");
        exp1.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.save(exp1);

        DataErasureJob era1 = new DataErasureJob();
        era1.setRequestId("e2e-era-fd6672");
        era1.setSubjectId(subject1);
        era1.setStatus("PROCESSING");
        era1.setConfirmationToken("CONFIRM_FD6672");
        era1.setReason("Stuck subject test");
        era1.setErasureScope("ALL_PERSONAL_DATA");
        era1.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.save(era1);

        DataExportJob exp2 = new DataExportJob();
        exp2.setRequestId("e2e-exp-765d2");
        exp2.setSubjectId(subject2);
        exp2.setStatus("PENDING");
        exp2.setRequestedFormat("ZIP");
        exp2.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.save(exp2);

        DataErasureJob era2 = new DataErasureJob();
        era2.setRequestId("e2e-era-765d2");
        era2.setSubjectId(subject2);
        era2.setStatus("PROCESSING");
        era2.setConfirmationToken("CONFIRM_765D2");
        era2.setReason("Stuck subject test 2");
        era2.setErasureScope("ALL_PERSONAL_DATA");
        era2.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.save(era2);

        entityManager.flush();

        // Run previous conflicting migrations to simulate full ordered migration chain
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            runScript(conn, "db/migration/V20260823055044137__resolve_stuck_subject_fd6672c6.sql");
            runScript(conn, "db/migration/V20260823065127618__unblock_stuck_subjects.sql");
            runScript(conn, "db/migration/V20260823075743029__resolve_stuck_operations_auditor_subjects.sql");
            runScript(conn, "db/migration/V20260823075746149__reconcile_orchestrator_subjects_blocker.sql");
            runScript(conn, "db/migration/V20260823081040819__escalate_stuck_subjects.sql");
            runScript(conn, "db/migration/V20260823090157407__flag_stuck_subjects_for_human_review.sql");
            // Execute the new patch migration
            runScript(conn, "db/migration/V20260823204720699__unblock_stuck_auditor_subjects_and_escalate.sql");
        } catch (Exception e) {
            fail("Failed executing migration chain: " + e.getMessage());
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        entityManager.clear();

        // Assert terminal ESCALATED state for both subjects
        DataExportJob updatedExp1 = exportJobRepository.findById("e2e-exp-fd6672").orElseThrow();
        assertEquals("ESCALATED", updatedExp1.getStatus());

        DataErasureJob updatedEra1 = erasureJobRepository.findById("e2e-era-fd6672").orElseThrow();
        assertEquals("ESCALATED", updatedEra1.getStatus());

        DataExportJob updatedExp2 = exportJobRepository.findById("e2e-exp-765d2").orElseThrow();
        assertEquals("ESCALATED", updatedExp2.getStatus());

        DataErasureJob updatedEra2 = erasureJobRepository.findById("e2e-era-765d2").orElseThrow();
        assertEquals("ESCALATED", updatedEra2.getStatus());
    }
}
