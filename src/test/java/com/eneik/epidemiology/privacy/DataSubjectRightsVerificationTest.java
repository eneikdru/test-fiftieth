package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DataSubjectRightsVerificationTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private PrivacyService privacyService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-22T15:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        privacyService = new PrivacyService(
            exportJobRepository,
            erasureJobRepository,
            userRepository,
            objectMapper,
            fixedClock
        );
    }

    @Test
    @DisplayName("Given a stuck subject record from task ea2d1954-4da4-4912-8565-dcd27a569279, When updated atomically, Then it transitions out of PENDING/PROCESSING to a resolvable state")
    void testStuckSubjectAtomicallyTransitionsToResolvableState() {
        String stuckSubjectId = "fd6672c6-02c4-455e-a4d9-91e4ae9d308c";
        String requestId = "stuck-export-request-1";

        DataExportJob stuckJob = new DataExportJob();
        stuckJob.setRequestId(requestId);
        stuckJob.setSubjectId(stuckSubjectId);
        stuckJob.setStatus("PENDING");
        stuckJob.setRequestedFormat("ZIP");
        stuckJob.setCreatedAt(OffsetDateTime.now(fixedClock));
        exportJobRepository.save(stuckJob);

        entityManager.flush();

        // Verify subject is initialised in stuck/pending state
        DataExportJob fetchedInitial = exportJobRepository.findById(requestId).orElseThrow();
        assertEquals("PENDING", fetchedInitial.getStatus());

        // Perform atomically-guarded update
        int updatedCount = exportJobRepository.updateStatusToCompleted(
            requestId,
            "PENDING",
            "RESOLVED",
            "/api/v1/privacy/export-requests/" + requestId + "/download",
            "{}",
            OffsetDateTime.now(fixedClock),
            OffsetDateTime.now(fixedClock).plusDays(7)
        );

        assertEquals(1, updatedCount, "Exactly one row should be updated atomically");

        entityManager.flush();
        entityManager.clear();

        DataExportJob fetchedUpdated = exportJobRepository.findById(requestId).orElseThrow();
        assertEquals("RESOLVED", fetchedUpdated.getStatus());

        // Verify that auditor won't find active pending/processing jobs for this subject
        List<DataExportJob> activeJobs = exportJobRepository.findBySubjectIdAndStatusIn(
            stuckSubjectId,
            List.of("PENDING", "PROCESSING")
        );
        assertTrue(activeJobs.isEmpty(), "No active pending or processing jobs should remain for stuck subject");
    }

    @Test
    @DisplayName("Given a test user requests erasure, When the process finishes, Then subsequent database queries confirm the data is gone")
    void testUserErasureDataIsGoneFromDatabase() {
        User user = new User("rights_erasure_user", "hashed_pwd_999", "RESEARCHER");
        user = userRepository.save(user);
        Long userId = user.getId();

        // Confirm user exists prior to erasure
        assertTrue(userRepository.findById(userId).isPresent());
        assertTrue(userRepository.findByUsername("rights_erasure_user").isPresent());

        // Perform erasure
        String confirmationToken = "CONFIRM_ERASURE_rights_erasure_user";
        DataErasureJob job = privacyService.initiateDataErasure("rights_erasure_user", confirmationToken, "152-FZ Withdrawal", "ALL_PERSONAL_DATA");

        assertEquals("COMPLETED", job.getStatus());
        assertEquals(1, job.getRecordsErasedCount());

        // Subsequent database queries confirm user data is gone
        Optional<User> byId = userRepository.findById(userId);
        Optional<User> byUsername = userRepository.findByUsername("rights_erasure_user");

        assertTrue(byId.isEmpty(), "User ID query must confirm data is deleted");
        assertTrue(byUsername.isEmpty(), "User username query must confirm data is deleted");
    }

    @Test
    @DisplayName("Given an export request, When the file is inspected, Then it contains all expected user properties")
    void testExportFileContainsAllExpectedUserProperties() throws Exception {
        User user = new User("rights_export_user", "hashed_pwd_888", "ADMIN");
        user = userRepository.save(user);

        // Test export in JSON format
        DataExportJob jsonJob = privacyService.initiateDataExport("rights_export_user", "JSON", "Export for inspection");
        assertEquals("COMPLETED", jsonJob.getStatus());

        PrivacyService.DownloadData jsonDownload = privacyService.getExportDownloadData(jsonJob.getRequestId());
        assertEquals("application/json", jsonDownload.mediaType());

        String jsonPayload = new String(jsonDownload.bytes(), StandardCharsets.UTF_8);
        Map<String, Object> jsonProperties = objectMapper.readValue(jsonPayload, new TypeReference<>() {});

        assertTrue(jsonProperties.containsKey("id"), "Export payload must contain property 'id'");
        assertTrue(jsonProperties.containsKey("username"), "Export payload must contain property 'username'");
        assertTrue(jsonProperties.containsKey("role"), "Export payload must contain property 'role'");
        assertTrue(jsonProperties.containsKey("created_at"), "Export payload must contain property 'created_at'");

        assertEquals(user.getId().intValue(), ((Number) jsonProperties.get("id")).intValue());
        assertEquals("rights_export_user", jsonProperties.get("username"));
        assertEquals("ADMIN", jsonProperties.get("role"));
        assertNotNull(jsonProperties.get("created_at"));

        // Test export in ZIP format
        DataExportJob zipJob = privacyService.initiateDataExport("rights_export_user", "ZIP", "Zip Export");
        assertEquals("COMPLETED", zipJob.getStatus());

        PrivacyService.DownloadData zipDownload = privacyService.getExportDownloadData(zipJob.getRequestId());
        assertEquals("application/zip", zipDownload.mediaType());

        String extractedZipJson = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipDownload.bytes()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("personal_data.json".equals(entry.getName())) {
                    extractedZipJson = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        assertNotNull(extractedZipJson, "Zip archive must contain personal_data.json");
        Map<String, Object> zipProperties = objectMapper.readValue(extractedZipJson, new TypeReference<>() {});

        assertTrue(zipProperties.containsKey("id"));
        assertTrue(zipProperties.containsKey("username"));
        assertTrue(zipProperties.containsKey("role"));
        assertTrue(zipProperties.containsKey("created_at"));
        assertEquals("rights_export_user", zipProperties.get("username"));
    }
}
