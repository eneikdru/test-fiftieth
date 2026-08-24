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
class CategorizeDesignReviewConcernPatternTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given uncategorized design review concern event, When mandatory Flyway migration V20260824054955643 executes, Then assigns rootCausePatternId PATTERN-DESIGN-REVIEW-CONCERN-01 and resolves pending privacy requests")
    void testCategorizeDesignReviewConcernPatternMigrationExecutesAndAssignsRootCausePatternId() {
        // Seed test pending export and erasure requests representing uncategorized design review concern events
        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-cat-exp-1");
        exportJob.setSubjectId("test-subject-categorize-01");
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.save(exportJob);

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-cat-era-1");
        erasureJob.setSubjectId("test-subject-categorize-01");
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_CATEGORIZE");
        erasureJob.setReason("Review concerns test uncategorized");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.save(erasureJob);

        entityManager.flush();

        // Execute mandatory Flyway migration script V20260824054955643
        try {
            ClassPathResource resource = new ClassPathResource("db/migration/V20260824054955643__categorize_design_review_concern_pattern.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            fail("Migration V20260824054955643 execution failed: " + e.getMessage());
        }

        entityManager.clear();

        // Assert that the pending/processing requests were transitioned to RESOLVED and carry rootCausePatternId
        DataExportJob updatedExport = exportJobRepository.findById("test-cat-exp-1").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());
        assertTrue(updatedExport.getNotes().contains("PATTERN-DESIGN-REVIEW-CONCERN-01"),
                "Export job notes must assign rootCausePatternId PATTERN-DESIGN-REVIEW-CONCERN-01");

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-cat-era-1").orElseThrow();
        assertEquals("RESOLVED", updatedErasure.getStatus());
        assertTrue(updatedErasure.getReason().contains("PATTERN-DESIGN-REVIEW-CONCERN-01"),
                "Erasure job reason must assign rootCausePatternId PATTERN-DESIGN-REVIEW-CONCERN-01");

        // Verify no pending or processing requests remain for subject
        List<DataExportJob> pendingExports = exportJobRepository.findBySubjectIdAndStatusIn("test-subject-categorize-01", List.of("PENDING", "PROCESSING"));
        assertTrue(pendingExports.isEmpty(), "No pending or processing export requests should remain");
    }
}
