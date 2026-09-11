package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA Verification for Hard Delete (BARCAN-TAG-06).
 * Verifies that account deletion permanently erases all user data and related records
 * from database tables, ensuring privacy compliance (152-FZ / GDPR).
 */
@SpringBootTest
@Transactional
class HardDeleteQaVerificationTest {

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

    @Autowired
    private EntityManager entityManager;

    private PrivacyService privacyService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-09-11T10:00:00Z"), ZoneOffset.UTC);

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
    @DisplayName("Given an existing user with related documents and reports, When account erasure is executed, Then querying database for user returns no results")
    void verifyHardDeleteUserReturnsNoResults() {
        // Arrange
        String username = "hard_delete_target_user";
        User user = new User(username, "pass_hash_123", "RESEARCHER", "hd_user@epid.org", "Hard Delete Test User", "moodle_hd_01", "Epidemiology", "COURSE_HD");
        user = userRepository.saveAndFlush(user);
        Long userId = user.getId();

        EmployeeDocument doc = new EmployeeDocument(username, "Research Paper", "Virology Study 2026", LocalDate.of(2026, 1, 10), "Sample analysis");
        employeeDocumentRepository.saveAndFlush(doc);

        DossierReport report = new DossierReport(username, "ANNUAL_SUMMARY", "GENERATED", "Annual Summary Report", 1, "/api/v1/dossier/download/hd_01");
        dossierReportRepository.saveAndFlush(report);

        entityManager.clear();

        // Verify pre-conditions
        assertTrue(userRepository.findById(userId).isPresent(), "User must exist in database prior to erasure");
        assertTrue(userRepository.findByUsername(username).isPresent(), "User must be searchable by username prior to erasure");

        // Act
        String token = "CONFIRM_ERASURE_" + username;
        DataErasureJob job = privacyService.initiateDataErasure(username, token, "Hard Delete Compliance Test", "ALL_PERSONAL_DATA");

        entityManager.flush();
        entityManager.clear();

        // Assert - AC1: Querying database for user returns no results
        assertEquals("COMPLETED", job.getStatus(), "Erasure job status must be COMPLETED");
        assertEquals(3, job.getRecordsErasedCount(), "Must record 3 erased records (1 user + 1 doc + 1 report)");

        Optional<User> userById = userRepository.findById(userId);
        Optional<User> userByUsername = userRepository.findByUsername(username);

        assertTrue(userById.isEmpty(), "Querying database by user ID must return no results after hard delete");
        assertTrue(userByUsername.isEmpty(), "Querying database by username must return no results after hard delete");
    }

    @Test
    @DisplayName("Given deleted user's ID, When querying related tables, Then no associated data is found")
    void verifyHardDeleteRelatedTablesReturnNoData() {
        // Arrange
        String username = "hard_delete_rel_user";
        User user = new User(username, "pass_hash_456", "RESEARCHER", "hd_rel@epid.org", "Related Data Test User", "moodle_hd_02", "Epidemiology", "COURSE_HD");
        user = userRepository.saveAndFlush(user);
        Long userId = user.getId();

        EmployeeDocument doc1 = new EmployeeDocument(username, "Order", "Order #1001", LocalDate.of(2026, 2, 1), "Details 1");
        EmployeeDocument doc2 = new EmployeeDocument(username, "Certificate", "Cert #2002", LocalDate.of(2026, 2, 15), "Details 2");
        employeeDocumentRepository.saveAndFlush(doc1);
        employeeDocumentRepository.saveAndFlush(doc2);

        DossierReport report1 = new DossierReport(username, "MONTHLY_SUMMARY", "GENERATED", "Monthly Summary", 2, "/api/v1/dossier/download/hd_02");
        dossierReportRepository.saveAndFlush(report1);

        entityManager.clear();

        // Verify pre-conditions: related tables contain user's data
        List<EmployeeDocument> preDocs = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc(username);
        List<DossierReport> preReports = dossierReportRepository.findByEmployeeId(username);
        assertEquals(2, preDocs.size(), "Employee documents must exist before deletion");
        assertEquals(1, preReports.size(), "Dossier reports must exist before deletion");

        // Act
        String token = "CONFIRM_ERASURE_" + username;
        privacyService.initiateDataErasure(username, token, "Hard Delete Compliance Test", "ALL_PERSONAL_DATA");

        entityManager.flush();
        entityManager.clear();

        // Assert - AC2: Given deleted user's ID, querying related tables returns no associated data
        List<EmployeeDocument> postDocs = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc(username);
        List<DossierReport> postReports = dossierReportRepository.findByEmployeeId(username);

        assertTrue(postDocs.isEmpty(), "Employee document table must contain no data associated with deleted user");
        assertTrue(postReports.isEmpty(), "Dossier report table must contain no data associated with deleted user");

        // Verify user table itself is also empty for this ID
        assertTrue(userRepository.findById(userId).isEmpty(), "User record must be removed");
    }
}
