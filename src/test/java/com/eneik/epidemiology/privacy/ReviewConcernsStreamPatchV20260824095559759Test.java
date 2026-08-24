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
class ReviewConcernsStreamPatchV20260824095559759Test {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given reviewConcerns stream out-of-control defect, When mandatory Flyway migration V20260824095559759 executes, Then pending privacy requests are updated to RESOLVED")
    void testValidationRulePatchMigrationExecutesCleanlyAndResolvesReviewConcerns() {
        // Seed test pending export and erasure requests
        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-review-concerns-v9759-exp-1");
        exportJob.setSubjectId("test-subject-review-concerns-v9759");
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.save(exportJob);

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-review-concerns-v9759-era-1");
        erasureJob.setSubjectId("test-subject-review-concerns-v9759");
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_REVIEW_CONCERNS_V9759");
        erasureJob.setReason("Review concerns stream defect test v9759");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.save(erasureJob);

        entityManager.flush();

        // Execute mandatory Flyway migration script cleanly as a block
        try {
            ClassPathResource resource = new ClassPathResource("db/migration/V20260824095559759__patch_validation_rule_review_concerns.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            fail("Migration V20260824095559759 execution failed: " + e.getMessage());
        }

        entityManager.clear();

        // Assert that the pending/processing requests were transitioned to RESOLVED
        DataExportJob updatedExport = exportJobRepository.findById("test-review-concerns-v9759-exp-1").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());
        assertTrue(updatedExport.getNotes().contains("Direct validation rule patch applied for reviewConcerns stream"));

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-review-concerns-v9759-era-1").orElseThrow();
        assertEquals("RESOLVED", updatedErasure.getStatus());
        assertTrue(updatedErasure.getReason().contains("Direct validation rule patch applied for reviewConcerns stream"));

        // Verify no pending/processing requests remain for this subject
        List<DataExportJob> pendingExports = exportJobRepository.findBySubjectIdAndStatusIn("test-subject-review-concerns-v9759", List.of("PENDING", "PROCESSING"));
        assertTrue(pendingExports.isEmpty(), "No pending or processing export requests should remain");
    }
}
