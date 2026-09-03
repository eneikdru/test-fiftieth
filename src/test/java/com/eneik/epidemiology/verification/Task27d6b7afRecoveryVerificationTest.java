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

class Task27d6b7afRecoveryVerificationTest {

    private RecoveryTaskRepository recoveryTaskRepository;
    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:24:24Z");

    @BeforeEach
    void setUp() {
        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover 27d6b7af context, When task is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testTask27d6b7afRecoveryEligibility() {
        RecoveryTask task27d6b7af = new RecoveryTask();
        task27d6b7af.setSubjectId("27d6b7af");
        task27d6b7af.setFailureReason("reconcileClosedUnmergedPullRequest: missing context on main branch");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task27d6b7af),
                "Task 27d6b7af with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given 27d6b7af recovery task in FAILED state, When resumeTask is executed, Then state transitions atomically to IN_PROGRESS")
    void testTask27d6b7afAtomicStateTransition() {
        UUID taskId = UUID.fromString("27d6b7af-0000-0000-0000-000000000000");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "27d6b7af",
                "API Slice 27d6b7af Recovery",
                "FAILED",
                "reconcileClosedUnmergedPullRequest failure",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask resumedTask = taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");

        assertNotNull(resumedTask);
        assertEquals("IN_PROGRESS", resumedTask.getStatus());
        assertEquals(now, resumedTask.getUpdatedAt());

        verify(recoveryTaskRepository, times(1))
                .updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", now);
    }

    @Test
    @DisplayName("Given a task in non-eligible state, When attempting recovery, Then TaskConflictException is thrown without atomic update")
    void testTask27d6b7afResumeInvalidState() {
        UUID taskId = UUID.fromString("27d6b7af-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        RecoveryTask completedTask = new RecoveryTask(
                taskId,
                "27d6b7af",
                "Completed API Slice 27d6b7af",
                "RESOLVED",
                "reconcileClosedUnmergedPullRequest: Completed",
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
    @DisplayName("Given concurrent modification when updating 27d6b7af task status, Then TaskConflictException is thrown")
    void testTask27d6b7afConcurrentUpdateFailure() {
        UUID taskId = UUID.fromString("27d6b7af-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "27d6b7af",
                "API Slice 27d6b7af",
                "FAILED",
                "reconcileClosedUnmergedPullRequest: missing context",
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

    @Test
    @DisplayName("Given 27d6b7af recovery task, When invalid action or non-existent task requested, Then proper domain exceptions are thrown")
    void testTask27d6b7afExceptionHandling() {
        UUID nonExistentId = UUID.fromString("27d6b7af-9999-9999-9999-999999999999");
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(TaskRecoveryService.TaskNotFoundException.class, () ->
                taskRecoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID taskId = UUID.fromString("27d6b7af-0000-0000-0000-000000000003");
        assertThrows(TaskRecoveryService.TaskBadRequestException.class, () ->
                taskRecoveryService.resumeTask(taskId, "UNSUPPORTED_ACTION")
        );
    }
}
