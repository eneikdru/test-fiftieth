package com.eneik.epidemiology.privacy;

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
class TaskRecoveryServiceTest {

    @Mock
    private RecoveryTaskRepository recoveryTaskRepository;

    private Clock fixedClock;
    private TaskRecoveryService taskRecoveryService;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-26T10:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given task failed due to reconcileClosedUnmergedPullRequest, When isEligibleRetiredPlanTask is evaluated, Then it returns true")
    void testIsEligibleRetiredPlanTask_MatchesReconcileFailure() {
        RecoveryTask task = new RecoveryTask();
        task.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task));
    }

    @Test
    @DisplayName("Given task failed due to poka-yoke retirement, When isEligibleRetiredPlanTask is evaluated, Then it returns true")
    void testIsEligibleRetiredPlanTask_MatchesPokaYokeFailure() {
        RecoveryTask task = new RecoveryTask();
        task.setFailureReason("Blocked task retired by iteration-admission poka-yoke; no child work created");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task));
    }

    @Test
    @DisplayName("Given task with other failure reason or null, When isEligibleRetiredPlanTask is evaluated, Then it returns false")
    void testIsEligibleRetiredPlanTask_DoesNotMatchOtherFailures() {
        RecoveryTask task1 = new RecoveryTask();
        task1.setFailureReason("Timeout error");
        assertFalse(taskRecoveryService.isEligibleRetiredPlanTask(task1));

        assertFalse(taskRecoveryService.isEligibleRetiredPlanTask(null));
    }

    @Test
    @DisplayName("Given valid task UUID, When resumeTask is called, Then task status is updated atomically to IN_PROGRESS")
    void testResumeTask_Success() {
        UUID taskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-26T09:00:00Z");
        RecoveryTask task = new RecoveryTask(
                taskId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6",
                "FAILED",
                "Failed due to reconcileClosedUnmergedPullRequest",
                createdAt,
                createdAt
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask result = taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC), result.getUpdatedAt());

        verify(recoveryTaskRepository, times(1))
                .updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Given non-existent task UUID, When resumeTask is called, Then TaskNotFoundException is thrown")
    void testResumeTask_NotFound() {
        UUID taskId = UUID.randomUUID();
        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.empty());

        TaskRecoveryService.TaskNotFoundException ex = assertThrows(
                TaskRecoveryService.TaskNotFoundException.class,
                () -> taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );

        assertEquals("TASK_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given invalid action parameter, When resumeTask is called, Then TaskBadRequestException is thrown")
    void testResumeTask_InvalidAction() {
        UUID taskId = UUID.randomUUID();

        TaskRecoveryService.TaskBadRequestException ex = assertThrows(
                TaskRecoveryService.TaskBadRequestException.class,
                () -> taskRecoveryService.resumeTask(taskId, "INVALID_ACTION")
        );

        assertEquals("INVALID_ACTION", ex.getErrorCode());
    }

    @Test
    @DisplayName("Given ineligible task, When resumeTask is called, Then TaskConflictException is thrown")
    void testResumeTask_IneligibleTask() {
        UUID taskId = UUID.randomUUID();
        RecoveryTask task = new RecoveryTask(
                taskId,
                "sub-1",
                "Task Title",
                "FAILED",
                "General Exception",
                OffsetDateTime.now(fixedClock),
                OffsetDateTime.now(fixedClock)
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskRecoveryService.TaskConflictException ex = assertThrows(
                TaskRecoveryService.TaskConflictException.class,
                () -> taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );

        assertEquals("STATE_CONFLICT", ex.getErrorCode());
    }
}
