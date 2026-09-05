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

class Task3b0a00eaRecoveryVerificationTest {

    private RecoveryTaskRepository recoveryTaskRepository;
    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-05T12:00:00Z");

    @BeforeEach
    void setUp() {
        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover 3b0a00ea context, When task is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testTask3b0a00eaRecoveryEligibility() {
        RecoveryTask task3b0a00ea = new RecoveryTask();
        task3b0a00ea.setSubjectId("3b0a00ea");
        task3b0a00ea.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build pipeline execution");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task3b0a00ea),
                "Task 3b0a00ea with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given 3b0a00ea recovery task in FAILED state, When resumeTask is executed, Then state transitions atomically to IN_PROGRESS")
    void testTask3b0a00eaAtomicStateTransition() {
        UUID taskId = UUID.fromString("3b0a00ea-0000-0000-0000-000000000000");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "3b0a00ea",
                "Test Coverage 3b0a00ea Recovery",
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
    void testTask3b0a00eaResumeInvalidState() {
        UUID taskId = UUID.fromString("3b0a00ea-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        RecoveryTask completedTask = new RecoveryTask(
                taskId,
                "3b0a00ea",
                "Completed Test Coverage 3b0a00ea",
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
    @DisplayName("Given concurrent modification when updating 3b0a00ea task status, Then TaskConflictException is thrown")
    void testTask3b0a00eaConcurrentUpdateFailure() {
        UUID taskId = UUID.fromString("3b0a00ea-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "3b0a00ea",
                "Test Coverage 3b0a00ea",
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
    @DisplayName("Given 3b0a00ea recovery task, When invalid action or non-existent task requested, Then proper domain exceptions are thrown")
    void testTask3b0a00eaExceptionHandling() {
        UUID nonExistentId = UUID.fromString("3b0a00ea-9999-9999-9999-999999999999");
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(TaskRecoveryService.TaskNotFoundException.class, () ->
                taskRecoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID taskId = UUID.fromString("3b0a00ea-0000-0000-0000-000000000003");
        assertThrows(TaskRecoveryService.TaskBadRequestException.class, () ->
                taskRecoveryService.resumeTask(taskId, "UNSUPPORTED_ACTION")
        );
    }
}
