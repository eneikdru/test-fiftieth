package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PrivacyServiceVerificationTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

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
            employeeDocumentRepository,
            dossierReportRepository,
            objectMapper,
            fixedClock
        );
    }

    // --- Happy Path (Positive Scenarios) ---
    @Test
    @DisplayName("Given an erasure request with valid confirmation token and associated dossier records, When executed, Then user and all dossier records are permanently removed")
    void testDataErasureE2EHappyPath() {
        User user = new User("e2e_erasure_target", "hash456", "RESEARCHER");
        user = userRepository.save(user);

        EmployeeDocument doc = new EmployeeDocument("e2e_erasure_target", "Order", "Order #123", LocalDate.of(2025, 1, 15), "Details");
        employeeDocumentRepository.save(doc);

        DossierReport report = new DossierReport("e2e_erasure_target", "MONTHLY_SUMMARY", "GENERATED", "Monthly report", 1, "/api/v1/dossier/download/2");
        dossierReportRepository.save(report);

        String token = "CONFIRM_ERASURE_e2e_erasure_target";
        DataErasureJob job = privacyService.initiateDataErasure("e2e_erasure_target", token, "152-FZ", "ALL_PERSONAL_DATA");

        assertNotNull(job);
        assertEquals("COMPLETED", job.getStatus());
        assertEquals(3, job.getRecordsErasedCount());

        Optional<User> erasedUser = userRepository.findByUsername("e2e_erasure_target");
        assertTrue(erasedUser.isEmpty(), "User data must be permanently removed from database");

        List<EmployeeDocument> remainingDocs = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc("e2e_erasure_target");
        assertTrue(remainingDocs.isEmpty(), "Employee documents must be permanently deleted from database");

        List<DossierReport> remainingReports = dossierReportRepository.findByEmployeeId("e2e_erasure_target");
        assertTrue(remainingReports.isEmpty(), "Dossier reports must be permanently deleted from database");
    }

    @Test
    @DisplayName("Given an employee with associated documents, When data export is initiated, Then complete personal data profile and documents are included in payload JSON perfectly matching stored data")
    void testDataExportE2EHappyPath() throws Exception {
        User user = new User("e2e_export_emp_1", "hash123", "RESEARCHER", "export1@epid.org", "Иванов Иван Иванович", "moodle_101", "Эпидемиология", "COURSE_101");
        userRepository.save(user);

        EmployeeDocument doc1 = new EmployeeDocument("e2e_export_emp_1", "Publication", "Virology Research Paper", LocalDate.of(2025, 5, 10), "Details on virology");
        doc1.setScientificDirection("вирусология");
        employeeDocumentRepository.save(doc1);

        DossierReport report = new DossierReport("e2e_export_emp_1", "ANNUAL_SUMMARY", "GENERATED", "Summary for 2025", 1, "/api/v1/dossier/download/1");
        dossierReportRepository.save(report);

        DataExportJob job = privacyService.initiateDataExport("e2e_export_emp_1", "JSON", "Export with dossier");
        assertEquals("COMPLETED", job.getStatus());

        PrivacyService.DownloadData downloadData = privacyService.getExportDownloadData(job.getRequestId());
        String payload = new String(downloadData.bytes(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<>() {});

        assertEquals("export1@epid.org", map.get("email"));
        assertEquals("Иванов Иван Иванович", map.get("full_name"));
        assertEquals("moodle_101", map.get("moodle_id"));
        assertEquals("Эпидемиология", map.get("department"));
        assertEquals("COURSE_101", map.get("courses"));

        assertTrue(map.containsKey("dossier_documents"));
        List<Map<String, Object>> docs = (List<Map<String, Object>>) map.get("dossier_documents");
        assertEquals(1, docs.size());
        assertEquals("Virology Research Paper", docs.get(0).get("title"));

        assertTrue(map.containsKey("dossier_reports"));
        List<Map<String, Object>> reports = (List<Map<String, Object>>) map.get("dossier_reports");
        assertEquals(1, reports.size());
        assertEquals("ANNUAL_SUMMARY", reports.get(0).get("template_type"));
    }

    // --- Negative Scenarios ---
    @Test
    @DisplayName("Given blank subject id, When initiating erasure, Then INVALID_SUBJECT_ID exception is thrown")
    void testErasureBlankSubjectId() {
        PrivacyService.PrivacyException ex = assertThrows(PrivacyService.PrivacyException.class, () ->
            privacyService.initiateDataErasure("", "some_token", "reason", "ALL_PERSONAL_DATA")
        );
        assertEquals("INVALID_SUBJECT_ID", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given non-existent subject id, When initiating erasure, Then SUBJECT_NOT_FOUND exception is thrown")
    void testErasureSubjectNotFound() {
        PrivacyService.PrivacyNotFoundException ex = assertThrows(PrivacyService.PrivacyNotFoundException.class, () ->
            privacyService.initiateDataErasure("ghost_user", "CONFIRM_ERASURE_ghost_user", "reason", "ALL_PERSONAL_DATA")
        );
        assertEquals("SUBJECT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given export job not ready, When downloading data, Then EXPORT_NOT_READY exception is thrown")
    void testDownloadExportNotReady() {
        DataExportJob pendingJob = new DataExportJob();
        pendingJob.setRequestId("job-export-pending");
        pendingJob.setSubjectId("user1");
        pendingJob.setStatus("PENDING");
        pendingJob.setRequestedFormat("ZIP");
        pendingJob.setCreatedAt(java.time.OffsetDateTime.now(fixedClock));
        exportJobRepository.save(pendingJob);

        PrivacyService.PrivacyException ex = assertThrows(PrivacyService.PrivacyException.class, () ->
            privacyService.getExportDownloadData("job-export-pending")
        );
        assertEquals("EXPORT_NOT_READY", ex.getErrorCode());
    }

    // --- Boundary Scenarios ---
    @Test
    @DisplayName("Given erasure request with exact active pending state, When initiating new erasure, Then ACTIVE_ERASURE_EXISTS exception is thrown to prevent concurrent deletion races")
    void testErasureActiveJobExistsBoundary() {
        User user = new User("active_erasure_user", "hash", "RESEARCHER");
        userRepository.save(user);

        DataErasureJob pendingJob = new DataErasureJob();
        pendingJob.setRequestId("job-erasure-111");
        pendingJob.setSubjectId("active_erasure_user");
        pendingJob.setStatus("PENDING");
        pendingJob.setConfirmationToken("CONFIRM_ERASURE_active_erasure_user");
        pendingJob.setCreatedAt(java.time.OffsetDateTime.now(fixedClock));
        erasureJobRepository.save(pendingJob);

        PrivacyService.PrivacyConflictException ex = assertThrows(PrivacyService.PrivacyConflictException.class, () ->
            privacyService.initiateDataErasure("active_erasure_user", "CONFIRM_ERASURE_active_erasure_user", "reason", "ALL_PERSONAL_DATA")
        );
        assertEquals("ACTIVE_ERASURE_EXISTS", ex.getErrorCode());
    }
}
