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
class PrivacyServiceTest {

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
    @DisplayName("Given an employee with associated documents, When data export is initiated, Then documents are included in payload JSON")
    void testDataExportWithEmployeeDocuments() throws Exception {
        User user = new User("export_emp_1", "hash123", "RESEARCHER");
        userRepository.save(user);

        EmployeeDocument doc1 = new EmployeeDocument("export_emp_1", "Publication", "Virology Research Paper", LocalDate.of(2025, 5, 10), "Details on virology");
        doc1.setScientificDirection("вирусология");
        employeeDocumentRepository.save(doc1);

        DossierReport report = new DossierReport("export_emp_1", "ANNUAL_SUMMARY", "GENERATED", "Summary for 2025", 1, "/api/v1/dossier/download/1");
        dossierReportRepository.save(report);

        DataExportJob job = privacyService.initiateDataExport("export_emp_1", "JSON", "Export with dossier");
        assertEquals("COMPLETED", job.getStatus());

        PrivacyService.DownloadData downloadData = privacyService.getExportDownloadData(job.getRequestId());
        String payload = new String(downloadData.bytes(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<>() {});

        assertTrue(map.containsKey("dossier_documents"));
        List<Map<String, Object>> docs = (List<Map<String, Object>>) map.get("dossier_documents");
        assertEquals(1, docs.size());
        assertEquals("Virology Research Paper", docs.get(0).get("title"));

        assertTrue(map.containsKey("dossier_reports"));
        List<Map<String, Object>> reports = (List<Map<String, Object>>) map.get("dossier_reports");
        assertEquals(1, reports.size());
        assertEquals("ANNUAL_SUMMARY", reports.get(0).get("template_type"));
    }

    @Test
    @DisplayName("Given an erasure request with valid confirmation token and associated dossier records, When executed, Then user and all dossier records are permanently removed")
    void testDataErasureSuccess() {
        User user = new User("erasure_target", "hash456", "RESEARCHER");
        user = userRepository.save(user);

        EmployeeDocument doc = new EmployeeDocument("erasure_target", "Order", "Order #123", LocalDate.of(2025, 1, 15), "Details");
        employeeDocumentRepository.save(doc);

        DossierReport report = new DossierReport("erasure_target", "MONTHLY_SUMMARY", "GENERATED", "Monthly report", 1, "/api/v1/dossier/download/2");
        dossierReportRepository.save(report);

        String token = "CONFIRM_ERASURE_erasure_target";
        DataErasureJob job = privacyService.initiateDataErasure("erasure_target", token, "152-FZ", "ALL_PERSONAL_DATA");

        assertNotNull(job);
        assertEquals("COMPLETED", job.getStatus());
        assertEquals(3, job.getRecordsErasedCount()); // 1 user + 1 doc + 1 report

        Optional<User> erasedUser = userRepository.findByUsername("erasure_target");
        assertTrue(erasedUser.isEmpty(), "User data must be permanently removed from database");

        List<EmployeeDocument> remainingDocs = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc("erasure_target");
        assertTrue(remainingDocs.isEmpty(), "Employee documents must be permanently deleted from database");

        List<DossierReport> remainingReports = dossierReportRepository.findByEmployeeId("erasure_target");
        assertTrue(remainingReports.isEmpty(), "Dossier reports must be permanently deleted from database");
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
