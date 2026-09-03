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

class Task12c70886RecoveryVerificationTest {

    private RecoveryTaskRepository recoveryTaskRepository;
    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T07:30:00Z");

    @BeforeEach
    void setUp() {
        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given request to recover 12c70886 context, When task is evaluated, Then failure reason is correctly recognized as eligible for recovery")
    void testTask12c70886RecoveryEligibility() {
        RecoveryTask task12c70886 = new RecoveryTask();
        task12c70886.setSubjectId("12c70886");
        task12c70886.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build pipeline execution");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task12c70886),
                "Task 12c70886 with reconcile failure reason must be eligible for recovery");
    }

    @Test
    @DisplayName("Given 12c70886 recovery task in FAILED state, When resumeTask is executed, Then state transitions atomically to IN_PROGRESS")
    void testTask12c70886AtomicStateTransition() {
        UUID taskId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "12c70886",
                "Merge Readiness 12c70886 Recovery",
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
    @DisplayName("Given 12c70886 recovery task, When invalid action or non-existent task requested, Then proper domain exceptions are thrown")
    void testTask12c70886ExceptionHandling() {
        UUID nonExistentId = UUID.randomUUID();
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(TaskRecoveryService.TaskNotFoundException.class, () ->
                taskRecoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );

        UUID taskId = UUID.randomUUID();
        assertThrows(TaskRecoveryService.TaskBadRequestException.class, () ->
                taskRecoveryService.resumeTask(taskId, "UNSUPPORTED_ACTION")
        );
    }
}
