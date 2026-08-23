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
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
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

    @Autowired
    private DataSource dataSource;

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
    @DisplayName("Given subject fd6672c6-02c4-455e-a4d9-91e4ae9d308c is stuck, When state patch runs, Then subject is explicitly escalated")
    void testStuckSubjectFd6672c6ExplicitlyEscalated() {
        String stuckSubjectId = "fd6672c6-02c4-455e-a4d9-91e4ae9d308c";
        String exportRequestId = "stuck-export-request-fd6672c6";
        String erasureRequestId = "stuck-erasure-request-fd6672c6";

        // Seed stuck export request in existing privacy_export_requests table
        DataExportJob stuckExport = new DataExportJob();
        stuckExport.setRequestId(exportRequestId);
        stuckExport.setSubjectId(stuckSubjectId);
        stuckExport.setStatus("PENDING");
        stuckExport.setRequestedFormat("ZIP");
        stuckExport.setCreatedAt(OffsetDateTime.now(fixedClock));
        exportJobRepository.save(stuckExport);

        // Seed stuck erasure request in existing privacy_erasure_requests table
        DataErasureJob stuckErasure = new DataErasureJob();
        stuckErasure.setRequestId(erasureRequestId);
        stuckErasure.setSubjectId(stuckSubjectId);
        stuckErasure.setStatus("PROCESSING");
        stuckErasure.setConfirmationToken("CONFIRM_TOKEN_STUCK");
        stuckErasure.setReason("In-progress erasure");
        stuckErasure.setErasureScope("ALL_PERSONAL_DATA");
        stuckErasure.setCreatedAt(OffsetDateTime.now(fixedClock));
        erasureJobRepository.save(stuckErasure);

        entityManager.flush();
        entityManager.clear();

        // Execute the Flyway migration script V20260823081040819__escalate_stuck_wishlist_subjects.sql
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V20260823081040819__escalate_stuck_wishlist_subjects.sql"));
        } catch (Exception e) {
            fail("Failed to execute migration script: " + e.getMessage());
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        entityManager.clear();

        // Verify states transitioned to ESCALATED
        DataExportJob fetchedExportUpdated = exportJobRepository.findById(exportRequestId).orElseThrow();
        assertEquals("ESCALATED", fetchedExportUpdated.getStatus());
        assertTrue(fetchedExportUpdated.getNotes().contains("Escalated stuck subject fd6672c6"));

        DataErasureJob fetchedErasureUpdated = erasureJobRepository.findById(erasureRequestId).orElseThrow();
        assertEquals("ESCALATED", fetchedErasureUpdated.getStatus());
        assertTrue(fetchedErasureUpdated.getReason().contains("Escalated stuck subject fd6672c6"));
    }

    @Test
    @DisplayName("Given subject 765d2ab0-1b55-4701-babd-af5247442de5 is stuck, When state patch runs, Then subject is explicitly escalated")
    void testStuckSubject765d2ab0ExplicitlyEscalated() {
        String stuckSubjectId = "765d2ab0-1b55-4701-babd-af5247442de5";
        String exportRequestId = "stuck-export-request-765d2ab0";
        String erasureRequestId = "stuck-erasure-request-765d2ab0";

        // Seed stuck export request in existing privacy_export_requests table
        DataExportJob stuckExport = new DataExportJob();
        stuckExport.setRequestId(exportRequestId);
        stuckExport.setSubjectId(stuckSubjectId);
        stuckExport.setStatus("PENDING");
        stuckExport.setRequestedFormat("ZIP");
        stuckExport.setCreatedAt(OffsetDateTime.now(fixedClock));
        exportJobRepository.save(stuckExport);

        // Seed stuck erasure request in existing privacy_erasure_requests table
        DataErasureJob stuckErasure = new DataErasureJob();
        stuckErasure.setRequestId(erasureRequestId);
        stuckErasure.setSubjectId(stuckSubjectId);
        stuckErasure.setStatus("PROCESSING");
        stuckErasure.setConfirmationToken("CONFIRM_TOKEN_765D2AB0");
        stuckErasure.setReason("In-progress erasure");
        stuckErasure.setErasureScope("ALL_PERSONAL_DATA");
        stuckErasure.setCreatedAt(OffsetDateTime.now(fixedClock));
        erasureJobRepository.save(stuckErasure);

        entityManager.flush();
        entityManager.clear();

        // Execute the Flyway migration script V20260823081040819__escalate_stuck_wishlist_subjects.sql
        Connection conn2 = DataSourceUtils.getConnection(dataSource);
        try {
            ScriptUtils.executeSqlScript(conn2, new ClassPathResource("db/migration/V20260823081040819__escalate_stuck_wishlist_subjects.sql"));
        } catch (Exception e) {
            fail("Failed to execute migration script: " + e.getMessage());
        } finally {
            DataSourceUtils.releaseConnection(conn2, dataSource);
        }

        entityManager.clear();

        DataExportJob fetchedExport = exportJobRepository.findById(exportRequestId).orElseThrow();
        assertEquals("ESCALATED", fetchedExport.getStatus());

        DataErasureJob fetchedErasure = erasureJobRepository.findById(erasureRequestId).orElseThrow();
        assertEquals("ESCALATED", fetchedErasure.getStatus());
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
