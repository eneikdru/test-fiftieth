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

class Process27d6b7afRecoveryVerificationTest {

    private BackgroundProcessRepository recoveryProcessRepository;
    private ProcessRecoveryService processRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:24:24Z");

    @BeforeEach
    void setUp() {
        recoveryProcessRepository = mock(BackgroundProcessRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        processRecoveryService = new ProcessRecoveryService(recoveryProcessRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover 27d6b7af context, When process is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testProcess27d6b7afRecoveryEligibility() {
        BackgroundProcess process27d6b7af = new BackgroundProcess();
        process27d6b7af.setSubjectId("27d6b7af");
        process27d6b7af.setFailureReason("data_processing_error: missing context on main branch");

        assertTrue(processRecoveryService.isEligibleRetiredPlanProcess(process27d6b7af),
                "Process 27d6b7af with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given 27d6b7af recovery process in FAILED state, When resumeProcess is executed, Then state transitions atomically to IN_PROGRESS")
    void testProcess27d6b7afAtomicStateTransition() {
        UUID processId = UUID.fromString("27d6b7af-0000-0000-0000-000000000000");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        BackgroundProcess process = new BackgroundProcess(
                processId,
                "27d6b7af",
                "API Slice 27d6b7af Recovery",
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
    void testProcess27d6b7afResumeInvalidState() {
        UUID processId = UUID.fromString("27d6b7af-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        BackgroundProcess completedProcess = new BackgroundProcess(
                processId,
                "27d6b7af",
                "Completed API Slice 27d6b7af",
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
    @DisplayName("Given concurrent modification when updating 27d6b7af process status, Then ProcessConflictException is thrown")
    void testProcess27d6b7afConcurrentUpdateFailure() {
        UUID processId = UUID.fromString("27d6b7af-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        BackgroundProcess failedProcess = new BackgroundProcess(
                processId,
                "27d6b7af",
                "API Slice 27d6b7af",
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
    @DisplayName("Given 27d6b7af recovery process, When invalid action or non-existent process requested, Then proper domain exceptions are thrown")
    void testProcess27d6b7afExceptionHandling() {
        UUID nonExistentId = UUID.fromString("27d6b7af-9999-9999-9999-999999999999");
        when(recoveryProcessRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ProcessRecoveryService.ProcessNotFoundException.class, () ->
                processRecoveryService.resumeProcess(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID processId = UUID.fromString("27d6b7af-0000-0000-0000-000000000003");
        assertThrows(ProcessRecoveryService.ProcessBadRequestException.class, () ->
                processRecoveryService.resumeProcess(processId, "UNSUPPORTED_ACTION")
        );
    }
}
