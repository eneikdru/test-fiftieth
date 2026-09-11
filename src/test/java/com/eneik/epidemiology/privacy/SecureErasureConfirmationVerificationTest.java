package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SecureErasureConfirmationVerificationTest {

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private DataErasureJobRepository erasureJobRepository;

    @Autowired
    private DataErasureTokenRepository erasureTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private PrivacyService privacyService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-09-11T10:00:00Z"), ZoneId.of("UTC"));

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
    @DisplayName("Given an erasure confirmation attempt, When using a deterministic string, Then the request is rejected")
    void testDeterministicTokenRejected() {
        User user = new User("deterministic_user", "hashed_pwd_123", "RESEARCHER");
        userRepository.save(user);

        // Attempt erasure with deterministic CONFIRM_ERASURE_ string
        String deterministicToken = "CONFIRM_ERASURE_deterministic_user";

        PrivacyService.PrivacyBadRequestException exception = assertThrows(
            PrivacyService.PrivacyBadRequestException.class,
            () -> privacyService.initiateDataErasure("deterministic_user", deterministicToken, "152-FZ Request", "ALL_PERSONAL_DATA")
        );

        assertEquals("INVALID_CONFIRMATION_TOKEN", exception.getErrorCode());
        // Verify user data remains intact in database
        assertTrue(userRepository.findByUsername("deterministic_user").isPresent());
    }

    @Test
    @DisplayName("Given a valid secure token, When submitted, Then the erasure is successfully confirmed")
    void testValidSecureTokenSucceeds() {
        User user = new User("secure_erasure_user", "hashed_pwd_456", "RESEARCHER");
        user = userRepository.save(user);

        EmployeeDocument doc = new EmployeeDocument("secure_erasure_user", "Article", "Epi Study", LocalDate.of(2026, 1, 1), "Details");
        employeeDocumentRepository.save(doc);

        String secureTokenStr = "sec_tok_3f9a71b28c82410ea098412409d2e051";

        DataErasureToken tokenEntity = new DataErasureToken();
        tokenEntity.setSubjectId("secure_erasure_user");
        tokenEntity.setToken(secureTokenStr);
        tokenEntity.setCreatedAt(OffsetDateTime.now(fixedClock));
        tokenEntity.setExpiresAt(OffsetDateTime.now(fixedClock).plusMinutes(30));
        tokenEntity.setUsed(false);
        erasureTokenRepository.saveAndFlush(tokenEntity);

        // Submit erasure with valid secure token
        DataErasureJob job = privacyService.initiateDataErasure("secure_erasure_user", secureTokenStr, "Valid Request", "ALL_PERSONAL_DATA");

        assertNotNull(job);
        assertEquals("COMPLETED", job.getStatus());
        assertEquals(2, job.getRecordsErasedCount()); // 1 user + 1 document

        // Verify user and documents are erased
        assertTrue(userRepository.findByUsername("secure_erasure_user").isEmpty());
        assertTrue(employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc("secure_erasure_user").isEmpty());

        // Verify token is marked as used
        Optional<DataErasureToken> updatedToken = erasureTokenRepository.findByToken(secureTokenStr);
        assertTrue(updatedToken.isPresent());
        assertTrue(updatedToken.get().getUsed());
    }

    @Test
    @DisplayName("Given an expired or already used secure token, When submitted, Then the request is rejected")
    void testExpiredOrUsedSecureTokenRejected() {
        User user = new User("reuse_user", "hashed_pwd_789", "RESEARCHER");
        userRepository.save(user);

        // Used token case
        String usedTokenStr = "sec_tok_used_123456789";
        DataErasureToken usedToken = new DataErasureToken();
        usedToken.setSubjectId("reuse_user");
        usedToken.setToken(usedTokenStr);
        usedToken.setCreatedAt(OffsetDateTime.now(fixedClock).minusHours(1));
        usedToken.setExpiresAt(OffsetDateTime.now(fixedClock).plusMinutes(30));
        usedToken.setUsed(true);
        erasureTokenRepository.saveAndFlush(usedToken);

        PrivacyService.PrivacyBadRequestException usedEx = assertThrows(
            PrivacyService.PrivacyBadRequestException.class,
            () -> privacyService.initiateDataErasure("reuse_user", usedTokenStr, "Reuse Attempt", "ALL_PERSONAL_DATA")
        );
        assertEquals("TOKEN_ALREADY_USED", usedEx.getErrorCode());

        // Expired token case
        String expiredTokenStr = "sec_tok_expired_987654321";
        DataErasureToken expiredToken = new DataErasureToken();
        expiredToken.setSubjectId("reuse_user");
        expiredToken.setToken(expiredTokenStr);
        expiredToken.setCreatedAt(OffsetDateTime.now(fixedClock).minusHours(2));
        expiredToken.setExpiresAt(OffsetDateTime.now(fixedClock).minusMinutes(5));
        expiredToken.setUsed(false);
        erasureTokenRepository.saveAndFlush(expiredToken);

        PrivacyService.PrivacyBadRequestException expiredEx = assertThrows(
            PrivacyService.PrivacyBadRequestException.class,
            () -> privacyService.initiateDataErasure("reuse_user", expiredTokenStr, "Expired Attempt", "ALL_PERSONAL_DATA")
        );
        assertEquals("TOKEN_EXPIRED", expiredEx.getErrorCode());
    }
}
