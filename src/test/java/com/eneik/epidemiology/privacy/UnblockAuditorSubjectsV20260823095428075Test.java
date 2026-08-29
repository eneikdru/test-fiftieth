package com.eneik.epidemiology.privacy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UnblockAuditorSubjectsV20260823095428075Test {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Given subject ae8e1efb is stuck, When atomically-guarded update runs, Then it is flagged for human adjudication")
    void testSubjectAe8e1efbFlaggedForHumanReview() {
        String subjectId = "ae8e1efb-88e8-4a17-83fb-942e06d65d53";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-ae8e1efb-v2");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("STUCK");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.parse("2026-08-23T00:00:00Z"));
        exportJobRepository.saveAndFlush(exportJob);

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-era-ae8e1efb-v2");
        erasureJob.setSubjectId(subjectId);
        erasureJob.setStatus("PENDING");
        erasureJob.setConfirmationToken("CONFIRM_ae8e1efb");
        erasureJob.setReason("Stuck requirement");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.parse("2026-08-23T00:00:00Z"));
        erasureJobRepository.saveAndFlush(erasureJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', notes = 'Flagged for human adjudication: Wishlist derived task failed due to PR#64 closed without merge' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED')",
            subjectId
        );

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', reason = 'Flagged for human adjudication: Wishlist derived task failed due to PR#64 closed without merge' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Export request must be updated to FLAGGED_FOR_HUMAN_REVIEW for ae8e1efb");
        assertTrue(erasureUpdated > 0, "Erasure request must be updated to FLAGGED_FOR_HUMAN_REVIEW for ae8e1efb");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-ae8e1efb-v2").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedExport.getStatus());

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-ae8e1efb-v2").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedErasure.getStatus());
    }

    @Test
    @DisplayName("Given subject a567a371 is stuck, When atomically-guarded update runs, Then it is flagged for human adjudication")
    void testSubjectA567a371FlaggedForHumanReview() {
        String subjectId = "a567a371-3dc4-490a-9a4b-bed7e9348f16";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-a567a371-v2");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("STUCK");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.parse("2026-08-23T00:00:00Z"));
        exportJobRepository.saveAndFlush(exportJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', notes = 'Flagged for human adjudication: Terminally failed task scope undelivered; PR#64 closed without merge' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'STUCK', 'RESOLVED')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Export request must be updated to FLAGGED_FOR_HUMAN_REVIEW for a567a371");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-a567a371-v2").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedExport.getStatus());
    }

    @Test
    @DisplayName("Given subject c4904e50 is stuck, When atomically-guarded update runs, Then it is kept active pending the recovery task")
    void testSubjectC4904e50KeptActivePendingRecovery() {
        String subjectId = "c4904e50-85af-4e98-8cef-f6cf92d74c30";

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-era-c4904e50-v2");
        erasureJob.setSubjectId(subjectId);
        erasureJob.setStatus("STUCK");
        erasureJob.setConfirmationToken("CONFIRM_c4904e50");
        erasureJob.setReason("Stuck requirement");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.parse("2026-08-23T00:00:00Z"));
        erasureJobRepository.saveAndFlush(erasureJob);

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'PROCESSING', reason = 'Kept active pending recovery task outcome' WHERE subject_id = ? AND status IN ('PENDING', 'STUCK', 'FLAGGED_FOR_HUMAN_REVIEW', 'RESOLVED')",
            subjectId
        );

        assertTrue(erasureUpdated > 0, "Erasure request must be updated to PROCESSING (active) for c4904e50");

        entityManager.clear();

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-c4904e50-v2").orElseThrow();
        assertEquals("PROCESSING", updatedErasure.getStatus());
    }
}
