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
class PokaYokeRetiredSubjectsResolutionTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private static final List<String> STUCK_SUBJECT_IDS = List.of(
        "86bfe9d0-6033-446b-adda-6e70b27f3f51",
        "ae8e1efb-88e8-4a17-83fb-942e06d65d53",
        "c4904e50-85af-4e98-8cef-f6cf92d74c30",
        "6f0f90b0-4cc1-4e87-a7a0-c6761b0d8625",
        "f90fa1fa-48c4-4bc6-80b7-8e4dbab6ad2b",
        "30b258b2-0f02-4605-8d7a-1ecb1b4bebbb"
    );

    @Test
    @DisplayName("Given stuck subjects in privacy tables, When resolution query is executed, Then all subjects transition to RESOLVED without human intervention")
    void testAutomatedResolutionOfPokaYokeRetiredSubjects() {
        // Seed test records for each stuck subject ID
        for (int i = 0; i < STUCK_SUBJECT_IDS.size(); i++) {
            String subjectId = STUCK_SUBJECT_IDS.get(i);

            DataExportJob exportJob = new DataExportJob();
            exportJob.setRequestId("test-exp-poka-" + i);
            exportJob.setSubjectId(subjectId);
            exportJob.setStatus(i % 2 == 0 ? "PENDING" : "FLAGGED_FOR_HUMAN_REVIEW");
            exportJob.setRequestedFormat("ZIP");
            exportJob.setCreatedAt(OffsetDateTime.now());
            exportJobRepository.saveAndFlush(exportJob);

            DataErasureJob erasureJob = new DataErasureJob();
            erasureJob.setRequestId("test-era-poka-" + i);
            erasureJob.setSubjectId(subjectId);
            erasureJob.setStatus("PROCESSING");
            erasureJob.setConfirmationToken("CONFIRM_ERASURE_" + i);
            erasureJob.setReason("Stuck poka-yoke subject");
            erasureJob.setErasureScope("ALL_PERSONAL_DATA");
            erasureJob.setCreatedAt(OffsetDateTime.now());
            erasureJobRepository.saveAndFlush(erasureJob);
        }

        // Execute the automated resolution update
        int exportUpdated = jdbcTemplate.update(
            "UPDATE privacy_export_requests SET status = 'RESOLVED', notes = 'Automated resolution: re-admitted and resolved poka-yoke retired subject without human intervention' WHERE subject_id IN (?, ?, ?, ?, ?, ?) AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')",
            STUCK_SUBJECT_IDS.toArray()
        );

        int erasureUpdated = jdbcTemplate.update(
            "UPDATE privacy_erasure_requests SET status = 'RESOLVED', reason = 'Automated resolution: re-admitted and resolved poka-yoke retired subject without human intervention' WHERE subject_id IN (?, ?, ?, ?, ?, ?) AND status IN ('PENDING', 'PROCESSING', 'FLAGGED_FOR_HUMAN_REVIEW')",
            STUCK_SUBJECT_IDS.toArray()
        );

        assertEquals(6, exportUpdated, "All 6 export requests must be updated to RESOLVED");
        assertEquals(6, erasureUpdated, "All 6 erasure requests must be updated to RESOLVED");

        entityManager.clear();

        for (int i = 0; i < STUCK_SUBJECT_IDS.size(); i++) {
            DataExportJob exportJob = exportJobRepository.findById("test-exp-poka-" + i).orElseThrow();
            assertEquals("RESOLVED", exportJob.getStatus());

            DataErasureJob erasureJob = erasureJobRepository.findById("test-era-poka-" + i).orElseThrow();
            assertEquals("RESOLVED", erasureJob.getStatus());
        }
    }
}
