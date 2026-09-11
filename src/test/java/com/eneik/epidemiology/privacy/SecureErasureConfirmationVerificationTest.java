package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.document.DossierReportRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

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
            erasureTokenRepository,
            userRepository,
            employeeDocumentRepository,
            dossierReportRepository,
            objectMapper,
            fixedClock
        );
    }

    @Test
    @DisplayName("Given an erasure confirmation attempt, When using a deterministic string, Then the request is rejected")
    void testDeterministicStringConfirmationFails() {
        User user = new User("deterministic_user", "pass123", "RESEARCHER");
        userRepository.save(user);

        // Plain deterministic string constructed from prefix + subjectId without a stored DataErasureToken challenge
        String deterministicToken = "CONFIRM_ERASURE_" + user.getUsername();

        PrivacyService.PrivacyBadRequestException exception = assertThrows(
            PrivacyService.PrivacyBadRequestException.class,
            () -> privacyService.initiateDataErasure(user.getUsername(), deterministicToken, "Withdraw consent", "ALL_PERSONAL_DATA")
        );

        assertEquals("INVALID_CONFIRMATION_TOKEN", exception.getErrorCode());
        assertTrue(userRepository.findByUsername("deterministic_user").isPresent(), "User should not be deleted when using unstored deterministic token");
    }

    @Test
    @DisplayName("Given a valid secure token, When submitted, Then the erasure is successfully confirmed")
    void testSecureTokenConfirmationSucceeds() {
        User user = new User("secure_token_user", "pass123", "RESEARCHER");
        userRepository.save(user);

        // Secure challenge token is cryptographically generated, stored in privacy_erasure_tokens table
        String secureTokenString = "SECURE_TOK_" + UUID.randomUUID();
        DataErasureToken tokenEntity = new DataErasureToken();
        tokenEntity.setSubjectId(user.getUsername());
        tokenEntity.setToken(secureTokenString);
        tokenEntity.setCreatedAt(OffsetDateTime.now(fixedClock));
        tokenEntity.setExpiresAt(OffsetDateTime.now(fixedClock).plusHours(24));
        tokenEntity.setUsed(false);
        erasureTokenRepository.saveAndFlush(tokenEntity);

        DataErasureJob job = privacyService.initiateDataErasure(user.getUsername(), secureTokenString, "Withdraw consent", "ALL_PERSONAL_DATA");

        assertNotNull(job);
        assertEquals("COMPLETED", job.getStatus());
        assertTrue(userRepository.findByUsername("secure_token_user").isEmpty(), "User should be permanently deleted after successful confirmation with secure token");

        DataErasureToken usedToken = erasureTokenRepository.findByToken(secureTokenString).orElseThrow();
        assertTrue(usedToken.getUsed(), "Token should be marked as used upon confirmation");
    }
}
