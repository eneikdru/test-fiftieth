package com.eneik.epidemiology.privacy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StuckSubjectsResolutionTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Given subject fd6672c6 is stuck in PENDING state, When atomically guarded update is executed, Then subject moves out of stuck state")
    void testResolveStuckSubjectFd6672() {
        String subjectId = "fd6672c6-02c4-455e-a4d9-91e4ae9d308c";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-fd6672");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.saveAndFlush(exportJob);

        DataErasureJob erasureJob = new DataErasureJob();
        erasureJob.setRequestId("test-era-fd6672");
        erasureJob.setSubjectId(subjectId);
        erasureJob.setStatus("PROCESSING");
        erasureJob.setConfirmationToken("CONFIRM_TOKEN");
        erasureJob.setReason("Retire poka yoke");
        erasureJob.setErasureScope("ALL_PERSONAL_DATA");
        erasureJob.setCreatedAt(OffsetDateTime.now());
        erasureJobRepository.saveAndFlush(erasureJob);

        // Execute atomically-guarded database UPDATE matching the Flyway migration
        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'RESOLVED', notes = 'Resolved stuck subject from iteration-admission poka-yoke failure' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'RESOLVED', reason = 'Resolved stuck subject from iteration-admission poka-yoke failure' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Targeted update must affect pending export request for subject fd6672c6");
        assertTrue(erasureUpdated > 0, "Targeted update must affect processing erasure request for subject fd6672c6");

        // Clear persistence context so entities are re-fetched from database
        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-fd6672").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());

        DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-fd6672").orElseThrow();
        assertEquals("RESOLVED", updatedErasure.getStatus());
    }

    @Test
    @DisplayName("Given subject 765d2ab0 is stuck with orphaned dependency, When atomically guarded update is executed, Then subject moves out of stuck state")
    void testResolveStuckSubject765d2() {
        String subjectId = "765d2ab0-1b55-4701-babd-af5247442de5";

        DataExportJob exportJob = new DataExportJob();
        exportJob.setRequestId("test-exp-765d2");
        exportJob.setSubjectId(subjectId);
        exportJob.setStatus("PENDING");
        exportJob.setRequestedFormat("ZIP");
        exportJob.setCreatedAt(OffsetDateTime.now());
        exportJobRepository.saveAndFlush(exportJob);

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'RESOLVED', notes = 'Resolved stuck subject with orphaned dependency' WHERE subject_id = ? AND status IN ('PENDING', 'PROCESSING')",
            subjectId
        );

        assertTrue(exportUpdated > 0, "Targeted update must affect pending export request for subject 765d2ab0");

        // Clear persistence context so entity is re-fetched from database
        entityManager.clear();

        DataExportJob updatedExport = exportJobRepository.findById("test-exp-765d2").orElseThrow();
        assertEquals("RESOLVED", updatedExport.getStatus());
    }
}
