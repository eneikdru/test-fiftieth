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

class TaskB6f6311bRecoveryVerificationTest {

    private RecoveryTaskRepository recoveryTaskRepository;
    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T11:12:00Z");

    @BeforeEach
    void setUp() {
        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover B6f6311b context, When task is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testTaskB6f6311bRecoveryEligibility() {
        RecoveryTask taskB6f6311b = new RecoveryTask();
        taskB6f6311b.setSubjectId("B6f6311b");
        taskB6f6311b.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build pipeline execution");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(taskB6f6311b),
                "Task B6f6311b with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given B6f6311b recovery task in FAILED state, When resumeTask is executed, Then state transitions atomically to IN_PROGRESS")
    void testTaskB6f6311bAtomicStateTransition() {
        UUID taskId = UUID.fromString("b6f6311b-0000-0000-0000-000000000000");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "B6f6311b",
                "API Slice B6f6311b Recovery",
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
    void testTaskB6f6311bResumeInvalidState() {
        UUID taskId = UUID.fromString("b6f6311b-0000-0000-0000-000000000001");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        RecoveryTask completedTask = new RecoveryTask(
                taskId,
                "B6f6311b",
                "Completed API Slice B6f6311b",
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
    @DisplayName("Given concurrent modification when updating B6f6311b task status, Then TaskConflictException is thrown")
    void testTaskB6f6311bConcurrentUpdateFailure() {
        UUID taskId = UUID.fromString("b6f6311b-0000-0000-0000-000000000002");
        OffsetDateTime initialTime = OffsetDateTime.now(fixedClock);
        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "B6f6311b",
                "API Slice B6f6311b",
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
    @DisplayName("Given B6f6311b recovery task, When invalid action or non-existent task requested, Then proper domain exceptions are thrown")
    void testTaskB6f6311bExceptionHandling() {
        UUID nonExistentId = UUID.fromString("b6f6311b-9999-9999-9999-999999999999");
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(TaskRecoveryService.TaskNotFoundException.class, () ->
                taskRecoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID taskId = UUID.fromString("b6f6311b-0000-0000-0000-000000000003");
        assertThrows(TaskRecoveryService.TaskBadRequestException.class, () ->
                taskRecoveryService.resumeTask(taskId, "UNSUPPORTED_ACTION")
        );
    }
}
