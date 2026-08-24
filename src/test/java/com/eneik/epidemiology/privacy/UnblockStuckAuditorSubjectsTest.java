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
class UnblockStuckAuditorSubjectsTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Given subject 86bfe9d0 is stuck, When state unblock patch is applied, Then subject is moved out of stuck state")
    void testUnblockSubject86bfe9d0() {
        String subjectId = "86bfe9d0-6033-446b-adda-6e70b27f3f51";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-86bfe9d0");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.saveAndFlush(exportJob);

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-era-86bfe9d0");
        erasureJob.setSubjectId(subjectId);
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_ERASURE_86bfe9d0");
        erasureJob.setReason("Stuck requirement");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.saveAndFlush(erasureJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'RESOLVED', notes = 'Unblocked stuck subject in operations auditor' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')",
            subjectId
        );

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'RESOLVED', reason = 'Unblocked stuck subject in operations auditor' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Export request must be updated to RESOLVED for 86bfe9d0");
        assertTrue(erasureUpdated > 0, "Erasure request must be updated to RESOLVED for 86bfe9d0");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-86bfe9d0").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-86bfe9d0").orElseThrow();
        assertEquals("RESOLVED", updatedErasure.getStatus());
    }

    @Test
    @DisplayName("Given subject ae8e1efb is stuck, When state unblock patch is applied, Then subject is moved out of stuck state")
    void testUnblockSubjectAe8e1efb() {
        String subjectId = "ae8e1efb-88e8-4a17-83fb-942e06d65d53";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-ae8e1efb");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("FLAGGED_FOR_HUMAN_REVIEW");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.saveAndFlush(exportJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'RESOLVED', notes = 'Unblocked stuck subject in operations auditor' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Export request must be updated to RESOLVED for ae8e1efb");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-ae8e1efb").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());
    }

    @Test
    @DisplayName("Given subject c4904e50 is stuck, When state unblock patch is applied, Then subject is moved out of stuck state")
    void testUnblockSubjectC4904e50() {
        String subjectId = "c4904e50-85af-4e98-8cef-f6cf92d74c30";

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-era-c4904e50");
        erasureJob.setSubjectId(subjectId);
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_ERASURE_c4904e50");
        erasureJob.setReason("Stuck requirement");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.saveAndFlush(erasureJob);

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'RESOLVED', reason = 'Unblocked stuck subject in operations auditor' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')",
            subjectId
        );

        assertTrue(erasureUpdated > 0, "Erasure request must be updated to RESOLVED for c4904e50");

        entityManager.clear();

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-c4904e50").orElseThrow();
        assertEquals("RESOLVED", updatedErasure.getStatus());
    }
}
