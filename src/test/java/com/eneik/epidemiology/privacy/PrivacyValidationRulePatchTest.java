package com.eneik.epidemiology.privacy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PrivacyValidationRulePatchTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given unhandled uncategorized task plan pattern triggers review concerns, When mandatory Flyway migration V20260824004540925 executes, Then pending privacy requests are resolved without failing validation")
    void testValidationRulePatchMigrationExecutesCleanlyAndResolvesPendingRequests() {
        // Seed test pending export and erasure requests
        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-patch-exp-1");
        exportJob.setSubjectId("test-subject-605eeda9");
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.save(exportJob);

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-patch-era-1");
        erasureJob.setSubjectId("test-subject-605eeda9");
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_PATCH");
        erasureJob.setReason("Review concerns test");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.save(erasureJob);

        entityManager.flush();

        // Execute mandatory Flyway migration script cleanly as a block
        try {
            ClassPathResource resource = new ClassPathResource("db/migration/V20260824004540925__patch_validation_rule_review_concerns.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            fail("Migration V20260824004540925 execution failed: " + e.getMessage());
        }

        entityManager.clear();

        // Assert that the pending/processing requests were transitioned to RESOLVED
        DataExportJob updatedExport = exportJobRepository.findById("test-patch-exp-1").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());
        assertTrue(updatedExport.getNotes().contains("Direct validation rule patch applied"));

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-patch-era-1").orElseThrow();
        assertEquals("RESOLVED", updatedErasure.getStatus());
        assertTrue(updatedErasure.getReason().contains("Direct validation rule patch applied"));

        // Verify no pending/processing requests remain
        List<DataExportJob> pendingExports = exportJobRepository.findBySubjectIdAndStatusIn("test-subject-605eeda9", List.of("PENDING", "PROCESSING"));
        assertTrue(pendingExports.isEmpty(), "No pending or processing export requests should remain");
    }
}
