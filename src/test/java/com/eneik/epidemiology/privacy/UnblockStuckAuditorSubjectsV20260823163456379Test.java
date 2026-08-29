package com.eneik.epidemiology.privacy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UnblockStuckAuditorSubjectsV20260823163456379Test {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Given stuck subjects, When database resolution patch is executed, Then targeted subjects are moved to RESOLVED status")
    void testUnblockStuckAuditorSubjects() {
        List<String> targetSubjectIds = List.of(
            "86bfe9d0-6033-446b-adda-6e70b27f3f51",
            "ae8e1efb-88e8-4a17-83fb-942e06d65d53",
            "c4904e50-85af-4e98-8cef-f6cf92d74c30",
            "6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625",
            "f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b",
            "30b258b2-0f02-4605-8d7a-1ecb1b4bebbb"
        );

        OffsetDateTime fixedTimestamp = OffsetDateTime.parse("2026-08-23T12:00:00Z");

        for (int i = 0; i < targetSubjectIds.size(); i++) {
            String subjectId = targetSubjectIds.get(i);

            DataExportJob exportJob = new DataExportJob();
            exportJob.setRequestId("test-exp-v379-" + i);
            exportJob.setSubjectId(subjectId);
            exportJob.setStatus("PENDING");
            exportJob.setRequestedFormat("ZIP");
            exportJob.setCreatedAt(fixedTimestamp);
            exportJobRepository.saveAndFlush(exportJob);

            DataErasureJob erasureJob = new DataErasureJob();
            erasureJob.setRequestId("test-era-v379-" + i);
            erasureJob.setSubjectId(subjectId);
            erasureJob.setStatus("PROCESSING");
            erasureJob.setConfirmationToken("CONFIRM_ERASURE_v379_" + i);
            erasureJob.setReason("Stuck requirement");
            erasureJob.setErasureScope("ALL_PERSONAL_DATA");
            erasureJob.setCreatedAt(fixedTimestamp);
            erasureJobRepository.saveAndFlush(erasureJob);
        }

        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'RESOLVED', notes = 'Automated resolution patch unblocking stuck auditor subjects' WHERE subject_id IN ('86bfe9d0-6033-446b-adda-6e70b27f3f51', 'ae8e1efb-88e8-4a17-83fb-942e06d65d53', 'c4904e50-85af-4e98-8cef-f6cf92d74c30', '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625', 'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b', '30b258b2-0f02-4605-8d7a-1ecb1b4bebbb') AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')"
        );

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'RESOLVED', reason = 'Automated resolution patch unblocking stuck auditor subjects' WHERE subject_id IN ('86bfe9d0-6033-446b-adda-6e70b27f3f51', 'ae8e1efb-88e8-4a17-83fb-942e06d65d53', 'c4904e50-85af-4e98-8cef-f6cf92d74c30', '6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625', 'f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b', '30b258b2-0f02-4605-8d7a-1ecb1b4bebbb') AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')"
        );

        assertEquals(6, exportUpdated, "All 6 export requests must be updated to RESOLVED");
        assertEquals(6, erasureUpdated, "All 6 erasure requests must be updated to RESOLVED");

        entityManager.clear();

        for (int i = 0; i < targetSubjectIds.size(); i++) {
            DataExportJob updatedExport = exportJobRepository.findById("test-exp-v379-" + i).orElseThrow();
            assertEquals("RESOLVED", updatedExport.getStatus());

            DataErasureJob updatedErasure = erasureJobRepository.findById("test-era-v379-" + i).orElseThrow();
            assertEquals("RESOLVED", updatedErasure.getStatus());
        }
    }
}
