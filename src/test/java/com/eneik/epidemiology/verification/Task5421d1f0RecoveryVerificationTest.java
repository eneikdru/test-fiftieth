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

class Task5421d1f0RecoveryVerificationTest {

    private RecoveryTaskRepository recoveryTaskRepository;
    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-04T12:00:00Z");

    @BeforeEach
    void setUp() {
        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover task 5421d1f0 context, When task is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testTask5421d1f0RecoveryEligibility() {
        RecoveryTask task5421d1f0 = new RecoveryTask();
        task5421d1f0.setSubjectId("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        task5421d1f0.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build pipeline execution");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task5421d1f0),
                "Task 5421d1f0 with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given 5421d1f0 recovery task in FAILED state, When resumeTask is executed, Then state transitions atomically to IN_PROGRESS")
    void testTask5421d1f0AtomicStateTransition() {
        UUID taskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6 Delivery",
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
    @DisplayName("Given 5421d1f0 recovery task, When invalid action or non-existent task requested, Then proper domain exceptions are thrown")
    void testTask5421d1f0ExceptionHandling() {
        UUID nonExistentId = UUID.fromString("5421d1f0-9999-9999-9999-999999999999");
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(TaskRecoveryService.TaskNotFoundException.class, () ->
                taskRecoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID taskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        assertThrows(TaskRecoveryService.TaskBadRequestException.class, () ->
                taskRecoveryService.resumeTask(taskId, "UNSUPPORTED_ACTION")
        );
    }
}
