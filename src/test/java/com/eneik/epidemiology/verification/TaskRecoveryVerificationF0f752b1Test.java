package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.privacy.RecoveryTask;
import com.eneik.epidemiology.privacy.RecoveryTaskRepository;
import com.eneik.epidemiology.privacy.TaskRecoveryService;
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

class TaskRecoveryVerificationF0f752b1Test {

    private RecoveryTaskRepository recoveryTaskRepository;
    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");

    @BeforeEach
    void setUp() {
        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given a failed recovery task, When resuming task slice, Then status transitions atomically to IN_PROGRESS")
    void testResumeFailedTaskRecovery() {
        UUID taskId = UUID.fromString("f0f752b1-0000-0000-0000-000000000000");
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "SUB-F0F752B1",
                "Data Processing Task F0f752b1 Context",
                "FAILED",
                "reconcileClosedUnmergedPullRequest: Data processing missing context",
                initialTime,
                initialTime
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(failedTask));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask resumed = taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");

        assertNotNull(resumed);
        assertEquals("IN_PROGRESS", resumed.getStatus());
        assertEquals(OffsetDateTime.now(fixedClock), resumed.getUpdatedAt());

        verify(recoveryTaskRepository, times(1))
                .updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", OffsetDateTime.now(fixedClock));
    }

    @Test
    @DisplayName("Given a task in non-eligible state, When attempting recovery, Then TaskConflictException is thrown without atomic update")
    void testResumeTaskInvalidState() {
        UUID taskId = UUID.fromString("f0f752b1-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        RecoveryTask completedTask = new RecoveryTask(
                taskId,
                "SUB-F0F752B1-2",
                "Completed Task Context",
                "RESOLVED",
                "reconcileClosedUnmergedPullRequest: Task completed successfully",
                initialTime,
                initialTime
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(completedTask));

        TaskRecoveryService.TaskConflictException exception = assertThrows(
                TaskRecoveryService.TaskConflictException.class,
                () -> taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );

        assertTrue(exception.getMessage().contains("Task is already in status: RESOLVED"));
        verify(recoveryTaskRepository, never()).updateStatusAtomically(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Given concurrent modification when updating task status, Then TaskConflictException is thrown")
    void testResumeTaskConcurrentUpdateFailure() {
        UUID taskId = UUID.fromString("f0f752b1-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.parse("2026-09-01T10:00:00Z");
        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "SUB-F0F752B1-3",
                "Data Processing Task F0f752b1",
                "FAILED",
                "reconcileClosedUnmergedPullRequest: Data processing missing context",
                initialTime,
                initialTime
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(failedTask));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(0);

        TaskRecoveryService.TaskConflictException exception = assertThrows(
                TaskRecoveryService.TaskConflictException.class,
                () -> taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );

        assertTrue(exception.getMessage().contains("Task status conflict during concurrent update"));
    }
}
