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

/**
 * QA Verification Suite for API Slice B6f6311b Recovery.
 * Verifies domain logic correctness, atomic guarded state transitions,
 * and exception boundaries without side effects.
 */
@ExtendWith(MockitoExtension.class)
class ApiSliceB6f6311bQaVerificationTest {

    @Mock
    private RecoveryTaskRepository recoveryTaskRepository;

    private Clock fixedClock;
    private TaskRecoveryService taskRecoveryService;

    private static final Instant TEST_INSTANT = Instant.parse("2026-09-03T12:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given a B6f6311b task with reconcile failure reason, When checked for recovery eligibility, Then returns true")
    void verifyTaskB6f6311bEligibilityPasses() {
        RecoveryTask task = new RecoveryTask();
        task.setSubjectId("B6f6311b");
        task.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build");

        boolean eligible = taskRecoveryService.isEligibleRetiredPlanTask(task);

        assertTrue(eligible, "Task with reconcileClosedUnmergedPullRequest failure must be eligible for recovery");
    }

    @Test
    @DisplayName("Given a B6f6311b task with non-reconcile failure reason, When checked for eligibility, Then returns false")
    void verifyTaskB6f6311bEligibilityFailsForUnrelatedReason() {
        RecoveryTask task = new RecoveryTask();
        task.setSubjectId("B6f6311b");
        task.setFailureReason("Unrelated network timeout");

        boolean eligible = taskRecoveryService.isEligibleRetiredPlanTask(task);

        assertFalse(eligible, "Task with non-reconcile failure reason must not be eligible");
    }

    @Test
    @DisplayName("Given an eligible FAILED task B6f6311b, When resumeTask is called, Then atomic update succeeds and status becomes IN_PROGRESS")
    void verifyAtomicStateTransitionSuccess() {
        UUID taskId = UUID.fromString("b6f6311b-1111-2222-3333-444455556666");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);

        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "B6f6311b",
                "API Slice B6f6311b Recovery",
                "FAILED",
                "Failed due to reconcileClosedUnmergedPullRequest",
                now.minusDays(1),
                now.minusDays(1)
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(failedTask));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask updatedTask = taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");

        assertNotNull(updatedTask, "Resumed task must not be null");
        assertEquals("IN_PROGRESS", updatedTask.getStatus(), "Status must transition to IN_PROGRESS");
        assertEquals(now, updatedTask.getUpdatedAt(), "Updated timestamp must reflect the injected fixed clock time");

        verify(recoveryTaskRepository, times(1))
                .updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", now);
    }

    @Test
    @DisplayName("Given concurrent modification during status update, When resumeTask is called, Then TaskConflictException is thrown")
    void verifyConcurrentUpdateThrowsConflictException() {
        UUID taskId = UUID.fromString("b6f6311b-1111-2222-3333-444455556667");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);

        RecoveryTask failedTask = new RecoveryTask(
                taskId,
                "B6f6311b",
                "API Slice B6f6311b Recovery",
                "FAILED",
                "Failed due to reconcileClosedUnmergedPullRequest",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(failedTask));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(0);

        TaskRecoveryService.TaskConflictException exception = assertThrows(
                TaskRecoveryService.TaskConflictException.class,
                () -> taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );

        assertEquals("STATE_CONFLICT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Task status conflict during concurrent update"));
    }

    @Test
    @DisplayName("Given a resolved task B6f6311b, When attempting to resume, Then TaskConflictException is thrown without atomic update")
    void verifyResolvedTaskCannotBeResumed() {
        UUID taskId = UUID.fromString("b6f6311b-1111-2222-3333-444455556668");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);

        RecoveryTask resolvedTask = new RecoveryTask(
                taskId,
                "B6f6311b",
                "API Slice B6f6311b Recovery",
                "RESOLVED",
                "Restored missing deliverable B6f6311b",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(resolvedTask));

        TaskRecoveryService.TaskConflictException exception = assertThrows(
                TaskRecoveryService.TaskConflictException.class,
                () -> taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );

        assertEquals("STATE_CONFLICT", exception.getErrorCode());
        verify(recoveryTaskRepository, never()).updateStatusAtomically(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Given invalid action or missing task ID, When resumeTask is invoked, Then appropriate domain exceptions are thrown")
    void verifyErrorHandlingForInvalidRequestAndMissingTask() {
        UUID nonExistentId = UUID.fromString("b6f6311b-9999-9999-9999-000000000000");
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        TaskRecoveryService.TaskNotFoundException notFoundEx = assertThrows(
                TaskRecoveryService.TaskNotFoundException.class,
                () -> taskRecoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );
        assertEquals("TASK_NOT_FOUND", notFoundEx.getErrorCode());

        UUID taskId = UUID.fromString("b6f6311b-1111-2222-3333-444455556669");
        TaskRecoveryService.TaskBadRequestException badRequestEx = assertThrows(
                TaskRecoveryService.TaskBadRequestException.class,
                () -> taskRecoveryService.resumeTask(taskId, "UNSUPPORTED_ACTION")
        );
        assertEquals("INVALID_ACTION", badRequestEx.getErrorCode());
    }
}
