package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * QA Verification Suite for Consent Enforcement and Privacy Boundaries.
 * Verifies that unconsented or invalid privacy/telemetry requests are rejected
 * cleanly at service boundaries.
 */
@ExtendWith(MockitoExtension.class)
class ConsentEnforcementQaVerificationTest {

    @Mock
    private DataExportJobRepository exportJobRepository;

    @Mock
    private DataErasureJobRepository erasureJobRepository;

    @Mock
    private UserRepository userRepository;

    private Clock fixedClock;
    private PrivacyService privacyService;

    private static final Instant TEST_INSTANT = Instant.parse("2026-09-13T12:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
        privacyService = new PrivacyService(
                exportJobRepository,
                erasureJobRepository,
                userRepository,
                new ObjectMapper(),
                fixedClock
        );
    }

    @Test
    @DisplayName("Given an erasure request without valid confirmation token, When initiateDataErasure is invoked, Then PrivacyBadRequestException is thrown and no data is erased")
    void testErasureWithoutValidConsentTokenFails() {
        User user = new User("test_user", "hashed_pwd", "RESEARCHER");
        when(userRepository.findByUsername("test_user")).thenReturn(Optional.of(user));

        PrivacyService.PrivacyBadRequestException exception = assertThrows(
                PrivacyService.PrivacyBadRequestException.class,
                () -> privacyService.initiateDataErasure("test_user", "INVALID_TOKEN", "152-FZ", "ALL")
        );

        assertEquals("INVALID_CONFIRMATION_TOKEN", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Неверный токен подтверждения"));

        verify(userRepository, never()).delete(any());
        verify(erasureJobRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Given a blank subject ID, When initiateDataExport is invoked, Then PrivacyException is thrown immediately")
    void testExportWithBlankSubjectIdFails() {
        PrivacyService.PrivacyException exception = assertThrows(
                PrivacyService.PrivacyException.class,
                () -> privacyService.initiateDataExport("", "ZIP", "Test notes")
        );

        assertEquals("INVALID_SUBJECT_ID", exception.getErrorCode());
        verify(exportJobRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Given an active erasure request in progress, When another erasure request is attempted, Then PrivacyConflictException is thrown")
    void testDuplicateErasureRequestFailsWithConflict() {
        User user = new User("active_user", "hashed_pwd", "RESEARCHER");
        when(userRepository.findByUsername("active_user")).thenReturn(Optional.of(user));

        DataErasureJob existingJob = new DataErasureJob();
        existingJob.setRequestId("job-123");
        existingJob.setStatus("PENDING");

        when(erasureJobRepository.findBySubjectIdAndStatusIn(eq("active_user"), anyList()))
                .thenReturn(List.of(existingJob));

        String validToken = "CONFIRM_ERASURE_active_user";

        PrivacyService.PrivacyConflictException exception = assertThrows(
                PrivacyService.PrivacyConflictException.class,
                () -> privacyService.initiateDataErasure("active_user", validToken, "152-FZ", "ALL")
        );

        assertEquals("ACTIVE_ERASURE_EXISTS", exception.getErrorCode());
        verify(userRepository, never()).delete(any());
    }
}
