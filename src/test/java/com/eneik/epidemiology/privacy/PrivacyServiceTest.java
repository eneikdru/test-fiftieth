package com.eneik.epidemiology.privacy;
import org.springframework.test.context.ContextConfiguration;
import com.eneik.epidemiology.PostgresTestContainerInitializer;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainerInitializer.class)
@Transactional
class PrivacyServiceTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("Given a valid user, When data export is initiated, Then job is completed and complete personal data package is returned")
    void testDataExportSuccess() {
        User user = new User("export_subject", "hash123", "RESEARCHER");
        user = userRepository.save(user);

        DataExportJob job = privacyService.initiateDataExport(user.getUsername(), "ZIP", "Test export");

        assertNotNull(job);
        assertNotNull(job.getRequestId());
        assertEquals("COMPLETED", job.getStatus());
        assertEquals("export_subject", job.getSubjectId());
        assertNotNull(job.getDownloadUrl());

        PrivacyService.DownloadData downloadData = privacyService.getExportDownloadData(job.getRequestId());
        assertNotNull(downloadData);
        assertEquals("application/zip", downloadData.mediaType());
        assertTrue(downloadData.bytes().length > 0);
    }

    @Test
    @DisplayName("Given active export request, When initiating duplicate request, Then conflict exception is thrown")
    void testDataExportConflict() {
        User user = new User("conflict_user", "hash123", "RESEARCHER");
        userRepository.save(user);

        DataExportJob pendingJob = new DataExportJob();
        pendingJob.setRequestId("job-111");
        pendingJob.setSubjectId("conflict_user");
        pendingJob.setStatus("PENDING");
        pendingJob.setRequestedFormat("ZIP");
        pendingJob.setCreatedAt(java.time.OffsetDateTime.now(fixedClock));
        exportJobRepository.save(pendingJob);

        assertThrows(PrivacyService.PrivacyConflictException.class, () ->
            privacyService.initiateDataExport("conflict_user", "ZIP", "Note")
        );
    }

    @Test
    @DisplayName("Given an erasure request with valid confirmation token, When executed, Then user is permanently removed from database")
    void testDataErasureSuccess() {
        User user = new User("erasure_target", "hash456", "RESEARCHER");
        user = userRepository.save(user);

        String token = "CONFIRM_ERASURE_erasure_target";
        DataErasureJob job = privacyService.initiateDataErasure("erasure_target", token, "152-FZ", "ALL_PERSONAL_DATA");

        assertNotNull(job);
        assertEquals("COMPLETED", job.getStatus());
        assertEquals(1, job.getRecordsErasedCount());

        Optional<User> erasedUser = userRepository.findByUsername("erasure_target");
        assertTrue(erasedUser.isEmpty(), "User data must be permanently removed from database");
    }

    @Test
    @DisplayName("Given an erasure request with invalid confirmation token, When submitted, Then bad request exception is thrown")
    void testDataErasureInvalidToken() {
        User user = new User("erasure_invalid_token", "hash789", "RESEARCHER");
        userRepository.save(user);

        assertThrows(PrivacyService.PrivacyBadRequestException.class, () ->
            privacyService.initiateDataErasure("erasure_invalid_token", "WRONG_TOKEN", "Reason", "ALL_PERSONAL_DATA")
        );
    }
}
