package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.process.BackgroundProcess;
import com.eneik.epidemiology.process.BackgroundProcessRepository;
import com.eneik.epidemiology.process.ProcessRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

class ProcessB6f6311bRecoveryVerificationTest {

    private BackgroundProcessRepository recoveryProcessRepository;
    private ProcessRecoveryService processRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T11:12:00Z");

    @BeforeEach
    void setUp() {
        recoveryProcessRepository = mock(BackgroundProcessRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        processRecoveryService = new ProcessRecoveryService(recoveryProcessRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover B6f6311b context, When process is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testProcessB6f6311bRecoveryEligibility() {
        BackgroundProcess processB6f6311b = new BackgroundProcess();
        processB6f6311b.setSubjectId("B6f6311b");
        processB6f6311b.setFailureReason("Process failed due to data_processing_error during build pipeline execution");

        assertTrue(processRecoveryService.isEligibleRetiredPlanProcess(processB6f6311b),
                "Process B6f6311b with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given B6f6311b recovery process in FAILED state, When resumeProcess is executed, Then state transitions atomically to IN_PROGRESS")
    void testProcessB6f6311bAtomicStateTransition() {
        UUID processId = UUID.fromString("b6f6311b-0000-0000-0000-000000000000");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        BackgroundProcess process = new BackgroundProcess(
                processId,
                "B6f6311b",
                "API Slice B6f6311b Recovery",
                "FAILED",
                "data_processing_error failure",
                now,
                now
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(process));
        when(recoveryProcessRepository.updateStatusAtomically(eq(processId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        BackgroundProcess resumedProcess = processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK");

        assertNotNull(resumedProcess);
        assertEquals("IN_PROGRESS", resumedProcess.getStatus());
        assertEquals(now, resumedProcess.getUpdatedAt());

        verify(recoveryProcessRepository, times(1))
                .updateStatusAtomically(processId, "FAILED", "IN_PROGRESS", now);
    }

    @Test
    @DisplayName("Given a process in non-eligible state, When attempting recovery, Then ProcessConflictException is thrown without atomic update")
    void testProcessB6f6311bResumeInvalidState() {
        UUID processId = UUID.fromString("b6f6311b-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        BackgroundProcess completedProcess = new BackgroundProcess(
                processId,
                "B6f6311b",
                "Completed API Slice B6f6311b",
                "RESOLVED",
                "data_processing_error: Completed",
                initialTime,
                initialTime
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(completedProcess));

        ProcessRecoveryService.ProcessConflictException exception = assertThrows(
                ProcessRecoveryService.ProcessConflictException.class,
                () -> processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK")
        );

        assertTrue(exception.getMessage().contains("Process is already in status: RESOLVED"));
        verify(recoveryProcessRepository, never()).updateStatusAtomically(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Given concurrent modification when updating B6f6311b process status, Then ProcessConflictException is thrown")
    void testProcessB6f6311bConcurrentUpdateFailure() {
        UUID processId = UUID.fromString("b6f6311b-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        BackgroundProcess failedProcess = new BackgroundProcess(
                processId,
                "B6f6311b",
                "API Slice B6f6311b",
                "FAILED",
                "data_processing_error: missing context",
                initialTime,
                initialTime
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(failedProcess));
        when(recoveryProcessRepository.updateStatusAtomically(eq(processId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(0);

        ProcessRecoveryService.ProcessConflictException exception = assertThrows(
                ProcessRecoveryService.ProcessConflictException.class,
                () -> processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK")
        );

        assertTrue(exception.getMessage().contains("Process status conflict during concurrent update"));
    }

    @Test
    @DisplayName("Given B6f6311b recovery process, When invalid action or non-existent process requested, Then proper domain exceptions are thrown")
    void testProcessB6f6311bExceptionHandling() {
        UUID nonExistentId = UUID.fromString("b6f6311b-9999-9999-9999-999999999999");
        when(recoveryProcessRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ProcessRecoveryService.ProcessNotFoundException.class, () ->
                processRecoveryService.resumeProcess(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID processId = UUID.fromString("b6f6311b-0000-0000-0000-000000000003");
        assertThrows(ProcessRecoveryService.ProcessBadRequestException.class, () ->
                processRecoveryService.resumeProcess(processId, "UNSUPPORTED_ACTION")
        );
    }
}
