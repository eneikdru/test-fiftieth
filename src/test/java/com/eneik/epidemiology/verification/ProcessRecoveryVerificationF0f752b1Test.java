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

class ProcessRecoveryVerificationF0f752b1Test {

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
    @DisplayName("Given a failed recovery process, When resuming process slice, Then status transitions atomically to IN_PROGRESS")
    void testResumeFailedProcessRecovery() {
        UUID processId = UUID.fromString("f0f752b1-0000-0000-0000-000000000000");
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        BackgroundProcess failedProcess = new BackgroundProcess(
                processId,
                "SUB-F0F752B1",
                "Data Processing Process F0f752b1 Context",
                "FAILED",
                "data_processing_error: Data processing missing context",
                initialTime,
                initialTime
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(failedProcess));
        when(recoveryProcessRepository.updateStatusAtomically(eq(processId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        BackgroundProcess resumed = processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK");

        assertNotNull(resumed);
        assertEquals("IN_PROGRESS", resumed.getStatus());
        assertEquals(OffsetDateTime.now(fixedClock), resumed.getUpdatedAt());

        verify(recoveryProcessRepository, times(1))
                .updateStatusAtomically(processId, "FAILED", "IN_PROGRESS", OffsetDateTime.now(fixedClock));
    }

    @Test
    @DisplayName("Given a process in non-eligible state, When attempting recovery, Then ProcessConflictException is thrown without atomic update")
    void testResumeProcessInvalidState() {
        UUID processId = UUID.fromString("f0f752b1-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        BackgroundProcess completedProcess = new BackgroundProcess(
                processId,
                "SUB-F0F752B1-2",
                "Completed Process Context",
                "RESOLVED",
                "data_processing_error: Process completed successfully",
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
    @DisplayName("Given concurrent modification when updating process status, Then ProcessConflictException is thrown")
    void testResumeProcessConcurrentUpdateFailure() {
        UUID processId = UUID.fromString("f0f752b1-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        BackgroundProcess failedProcess = new BackgroundProcess(
                processId,
                "SUB-F0F752B1-3",
                "Data Processing Process F0f752b1",
                "FAILED",
                "data_processing_error: Data processing missing context",
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
}
