package com.eneik.epidemiology.process;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessRecoveryServiceTest {

    @Mock
    private BackgroundProcessRepository recoveryProcessRepository;

    private Clock fixedClock;
    private ProcessRecoveryService processRecoveryService;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-26T10:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        processRecoveryService = new ProcessRecoveryService(recoveryProcessRepository, fixedClock);
    }

    @Test
    @DisplayName("Given process failed due to data_processing_error, When isEligibleRetiredPlanProcess is evaluated, Then it returns true")
    void testIsEligibleRetiredPlanProcess_MatchesReconcileFailure() {
        BackgroundProcess process = new BackgroundProcess();
        process.setFailureReason("Process failed due to data_processing_error during build");

        assertTrue(processRecoveryService.isEligibleRetiredPlanProcess(process));
    }

    @Test
    @DisplayName("Given process failed due to data-integrity retirement, When isEligibleRetiredPlanProcess is evaluated, Then it returns true")
    void testIsEligibleRetiredPlanProcess_MatchesPokaYokeFailure() {
        BackgroundProcess process = new BackgroundProcess();
        process.setFailureReason("Blocked process retired by iteration-admission data-integrity; no child work created");

        assertTrue(processRecoveryService.isEligibleRetiredPlanProcess(process));
    }

    @Test
    @DisplayName("Given process with other failure reason or null, When isEligibleRetiredPlanProcess is evaluated, Then it returns false")
    void testIsEligibleRetiredPlanProcess_DoesNotMatchOtherFailures() {
        BackgroundProcess process1 = new BackgroundProcess();
        process1.setFailureReason("Timeout error");
        assertFalse(processRecoveryService.isEligibleRetiredPlanProcess(process1));

        assertFalse(processRecoveryService.isEligibleRetiredPlanProcess(null));
    }

    @Test
    @DisplayName("Given valid process UUID, When resumeProcess is called, Then process status is updated atomically to IN_PROGRESS")
    void testResumeProcess_Success() {
        UUID processId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-26T09:00:00Z");
        BackgroundProcess process = new BackgroundProcess(
                processId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6",
                "FAILED",
                "Failed due to data_processing_error",
                createdAt,
                createdAt
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(process));
        when(recoveryProcessRepository.updateStatusAtomically(eq(processId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        BackgroundProcess result = processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK");

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC), result.getUpdatedAt());

        verify(recoveryProcessRepository, times(1))
                .updateStatusAtomically(processId, "FAILED", "IN_PROGRESS", OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Given non-existent process UUID, When resumeProcess is called, Then ProcessNotFoundException is thrown")
    void testResumeProcess_NotFound() {
        UUID processId = UUID.randomUUID();
        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.empty());

        ProcessRecoveryService.ProcessNotFoundException ex = assertThrows(
                ProcessRecoveryService.ProcessNotFoundException.class,
                () -> processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK")
        );

        assertEquals("PROCESS_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given invalid action parameter, When resumeProcess is called, Then ProcessBadRequestException is thrown")
    void testResumeProcess_InvalidAction() {
        UUID processId = UUID.randomUUID();

        ProcessRecoveryService.ProcessBadRequestException ex = assertThrows(
                ProcessRecoveryService.ProcessBadRequestException.class,
                () -> processRecoveryService.resumeProcess(processId, "INVALID_ACTION")
        );

        assertEquals("INVALID_ACTION", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given ineligible process, When resumeProcess is called, Then ProcessConflictException is thrown")
    void testResumeProcess_IneligibleProcess() {
        UUID processId = UUID.randomUUID();
        BackgroundProcess process = new BackgroundProcess(
                processId,
                "sub-1",
                "Process Title",
                "FAILED",
                "General Exception",
                OffsetDateTime.now(fixedClock),
                OffsetDateTime.now(fixedClock)
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(process));

        ProcessRecoveryService.ProcessConflictException ex = assertThrows(
                ProcessRecoveryService.ProcessConflictException.class,
                () -> processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK")
        );

        assertEquals("STATE_CONFLICT", ex.getErrorCode());
    }
}
