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

class Process3ce3cb21RecoveryVerificationTest {

    private BackgroundProcessRepository recoveryProcessRepository;
    private ProcessRecoveryService processRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");

    @BeforeEach
    void setUp() {
        recoveryProcessRepository = mock(BackgroundProcessRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        processRecoveryService = new ProcessRecoveryService(recoveryProcessRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover 3ce3cb21 context, When process is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testProcess3ce3cb21RecoveryEligibility() {
        BackgroundProcess process3ce3cb21 = new BackgroundProcess();
        process3ce3cb21.setSubjectId("3ce3cb21");
        process3ce3cb21.setFailureReason("Process failed due to data_processing_error during build pipeline execution");

        assertTrue(processRecoveryService.isEligibleRetiredPlanProcess(process3ce3cb21),
                "Process 3ce3cb21 with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given 3ce3cb21 recovery process in FAILED state, When resumeProcess is executed, Then state transitions atomically to IN_PROGRESS")
    void testProcess3ce3cb21AtomicStateTransition() {
        UUID processId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        BackgroundProcess process = new BackgroundProcess(
                processId,
                "3ce3cb21",
                "Merge Readiness 3ce3cb21 Recovery",
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
    @DisplayName("Given 3ce3cb21 recovery process, When invalid action or non-existent process requested, Then proper domain exceptions are thrown")
    void testProcess3ce3cb21ExceptionHandling() {
        UUID nonExistentId = UUID.randomUUID();
        when(recoveryProcessRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ProcessRecoveryService.ProcessNotFoundException.class, () ->
                processRecoveryService.resumeProcess(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID processId = UUID.randomUUID();
        assertThrows(ProcessRecoveryService.ProcessBadRequestException.class, () ->
                processRecoveryService.resumeProcess(processId, "UNSUPPORTED_ACTION")
        );
    }
}
