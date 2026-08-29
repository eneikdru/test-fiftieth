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
class RecoveredSubsetVerificationTest {

    @Mock
    private RecoveryTaskRepository recoveryTaskRepository;

    private TaskRecoveryService taskRecoveryService;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given task with reconcile unmerged PR failure reason, When checking eligibility, Then isEligibleRetiredPlanTask returns true")
    void verifyTaskRecoveryEligibility() {
        RecoveryTask task = new RecoveryTask();
        task.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build");
        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task), "Task matching failure pattern must be eligible");
    }

    @Test
    @DisplayName("Given task with unrelated failure reason, When checking eligibility, Then isEligibleRetiredPlanTask returns false")
    void verifyTaskRecoveryEligibility_Ineligible() {
        RecoveryTask task = new RecoveryTask();
        task.setFailureReason("General connection timeout");
        assertFalse(taskRecoveryService.isEligibleRetiredPlanTask(task), "Unrelated failure task must not be eligible");
    }

    @Test
    @DisplayName("Given eligible task UUID, When resumeTask is invoked, Then task status transitions to IN_PROGRESS atomically")
    void verifyTaskRecoveryStateTransition() {
        UUID taskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        OffsetDateTime now = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "Restored Service Work Item",
                "FAILED",
                "Task failed due to reconcileClosedUnmergedPullRequest",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask resumed = taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");
        assertNotNull(resumed, "Resumed task must not be null");
        assertEquals("IN_PROGRESS", resumed.getStatus(), "Task status must be updated to IN_PROGRESS");
        verify(recoveryTaskRepository, times(1))
                .updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", now);
    }
}
