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

class TaskRecoveryC0ee406bVerificationTest {

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
    @DisplayName("Given task C0ee406b slice restoring 5421d1f0 and 8bd0dbae, When evaluating eligibility, Then both are eligible for recovery")
    void testTaskC0ee406bRecoveryEligibility() {
        RecoveryTask task5421d1f0 = new RecoveryTask();
        task5421d1f0.setSubjectId("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        task5421d1f0.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build pipeline execution");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task5421d1f0),
                "Task 5421d1f0 within slice C0ee406b must be eligible for recovery");

        RecoveryTask task8bd0dbae = new RecoveryTask();
        task8bd0dbae.setSubjectId("8bd0dbae-41f6-466a-95a7-aff680ed0866");
        task8bd0dbae.setFailureReason("Task failed due to reconcileClosedUnmergedPullRequest during build pipeline execution");

        assertTrue(taskRecoveryService.isEligibleRetiredPlanTask(task8bd0dbae),
                "Task 8bd0dbae within slice C0ee406b must be eligible for recovery");
    }

    @Test
    @DisplayName("Given task C0ee406b slice components in FAILED status, When resumed, Then atomic state transitions succeed")
    void testTaskC0ee406bAtomicStateTransition() {
        UUID taskId5421d1f0 = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task1 = new RecoveryTask(
                taskId5421d1f0,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6 Delivery",
                "FAILED",
                "reconcileClosedUnmergedPullRequest failure",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId5421d1f0)).thenReturn(Optional.of(task1));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId5421d1f0), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask resumed1 = taskRecoveryService.resumeTask(taskId5421d1f0, "REVIVE_FAILED_TASK");

        assertNotNull(resumed1);
        assertEquals("IN_PROGRESS", resumed1.getStatus());

        UUID taskId8bd0dbae = UUID.fromString("8bd0dbae-41f6-466a-95a7-aff680ed0866");
        RecoveryTask task2 = new RecoveryTask(
                taskId8bd0dbae,
                "8bd0dbae-41f6-466a-95a7-aff680ed0866",
                "Runtime Contract 9b58412d Alignment",
                "FAILED",
                "reconcileClosedUnmergedPullRequest failure",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId8bd0dbae)).thenReturn(Optional.of(task2));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId8bd0dbae), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask resumed2 = taskRecoveryService.resumeTask(taskId8bd0dbae, "REVIVE_FAILED_TASK");

        assertNotNull(resumed2);
        assertEquals("IN_PROGRESS", resumed2.getStatus());
    }
}
