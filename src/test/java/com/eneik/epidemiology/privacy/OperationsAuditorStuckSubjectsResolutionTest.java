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
class OperationsAuditorStuckSubjectsResolutionTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Given subject 86bfe9d0 is stuck, When atomically guarded update is executed, Then subject is moved out of stuck state")
    void testResolveStuckSubject86bfe9d0() {
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
        erasureJob.setConfirmationToken("CONFIRM_TOKEN");
        erasureJob.setReason("Stuck subject");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.saveAndFlush(erasureJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', notes = 'Flagged for human review' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', reason = 'Flagged for human review' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Targeted update must affect pending export request for subject 86bfe9d0");
        assertTrue(erasureUpdated > 0, "Targeted update must affect processing erasure request for subject 86bfe9d0");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-86bfe9d0").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedExport.getStatus());

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-86bfe9d0").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedErasure.getStatus());
    }

    @Test
    @DisplayName("Given subject ae8e1efb is stuck, When atomically guarded update is executed, Then subject is moved out of stuck state")
    void testResolveStuckSubjectAe8e1efb() {
        String subjectId = "ae8e1efb-88e8-4a17-83fb-942e06d65d53";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-ae8e1efb");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.saveAndFlush(exportJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', notes = 'Flagged for human review' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Targeted update must affect pending export request for subject ae8e1efb");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-ae8e1efb").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedExport.getStatus());
    }

    @Test
    @DisplayName("Given subject c4904e50 is stuck, When atomically guarded update is executed, Then subject is moved out of stuck state")
    void testResolveStuckSubjectC4904e50() {
        String subjectId = "c4904e50-85af-4e98-8cef-f6cf92d74c30";

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-era-c4904e50");
        erasureJob.setSubjectId(subjectId);
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_TOKEN");
        erasureJob.setReason("Stuck subject");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.saveAndFlush(erasureJob);

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', reason = 'Flagged for human review' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        assertTrue(erasureUpdated > 0, "Targeted update must affect processing erasure request for subject c4904e50");

        entityManager.clear();

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-c4904e50").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedErasure.getStatus());
    }

    @Test
    @DisplayName("Given subject 96a47cb5 is stuck, When atomically guarded update is executed, Then subject is moved out of stuck state")
    void testResolveStuckSubject96a47cb5() {
        String subjectId = "96a47cb5-fc01-493f-b419-0b666dd5d713";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-96a47cb5");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.saveAndFlush(exportJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'FLAGGED_FOR_HUMAN_REVIEW', notes = 'Flagged for human review' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Targeted update must affect pending export request for subject 96a47cb5");

        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-96a47cb5").orElseThrow();
        assertEquals("FLAGGED_FOR_HUMAN_REVIEW", updatedExport.getStatus());
    }
}
