package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.categorization.CategorizationCoverageResponse;
import com.eneik.epidemiology.categorization.DesignReviewConcern;
import com.eneik.epidemiology.categorization.DesignReviewConcernRepository;
import com.eneik.epidemiology.categorization.RootCauseCategorizationService;
import com.eneik.epidemiology.categorization.RootCausePattern;
import com.eneik.epidemiology.categorization.RootCausePatternRepository;
import com.eneik.epidemiology.privacy.RecoveryTask;
import com.eneik.epidemiology.privacy.RecoveryTaskRepository;
import com.eneik.epidemiology.privacy.TaskRecoveryService;
import com.eneik.epidemiology.telemetry.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MergeReadinessRecoveryVerificationTest {

    private RootCauseCategorizationService categorizationService;
    private RootCausePatternRepository patternRepository;
    private DesignReviewConcernRepository concernRepository;
    private TelemetryService telemetryService;

    private TaskRecoveryService recoveryService;
    private RecoveryTaskRepository recoveryTaskRepository;

    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T12:00:00Z");

    @BeforeEach
    void setUp() {
        patternRepository = mock(RootCausePatternRepository.class);
        concernRepository = mock(DesignReviewConcernRepository.class);
        telemetryService = mock(TelemetryService.class);
        categorizationService = new RootCauseCategorizationService(patternRepository, concernRepository, telemetryService);

        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        recoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given null event, When evaluating schema event, Then returns false safely without exception")
    void testEvaluateExternalSchemaEventWithNull() {
        boolean result = categorizationService.evaluateExternalSchemaEvent(null);
        assertFalse(result);
    }

    @Test
    @DisplayName("Given concern already categorized, When categorizing in memory, Then skips reassignment")
    void testCategorizeConcernInMemoryAlreadyCategorized() {
        DesignReviewConcern concern = new DesignReviewConcern(
                "CONCERN-001", "reviewConcerns", 1, new BigDecimal("1.00"), "PATTERN-EXISTING", "CATEGORIZED", OffsetDateTime.now(fixedClock)
        );
        boolean updated = categorizationService.categorizeConcernInMemory(concern);
        assertFalse(updated);
        assertEquals("PATTERN-EXISTING", concern.getRootCausePatternId());
    }

    @Test
    @DisplayName("Given null concern, When categorizing in memory, Then returns false safely")
    void testCategorizeConcernInMemoryNull() {
        boolean updated = categorizationService.categorizeConcernInMemory(null);
        assertFalse(updated);
    }

    @Test
    @DisplayName("Given stream with uncategorized concerns, When categorizing review concerns, Then updates concerns atomically")
    void testCategorizeReviewConcernsAtomic() {
        String stream = "reviewConcerns";
        RootCausePattern pattern = new RootCausePattern(
                "RCP-REVIEW-CONCERNS-001", "Pattern Name", stream, "RULE-1", "RCP-REVIEW-CONCERNS-001", OffsetDateTime.now(fixedClock)
        );
        when(patternRepository.findByStreamName(stream)).thenReturn(Optional.of(pattern));

        DesignReviewConcern concern = new DesignReviewConcern(
                "CONCERN-002", stream, 1, new BigDecimal("1.00"), null, "UNCATEGORIZED", OffsetDateTime.now(fixedClock)
        );
        when(concernRepository.findByStreamNameAndRootCausePatternIdIsNull(stream)).thenReturn(List.of(concern));
        when(concernRepository.categorizeConcernAtomically("CONCERN-002", "RCP-REVIEW-CONCERNS-001")).thenReturn(1);

        int updatedCount = categorizationService.categorizeReviewConcerns(stream);
        assertEquals(1, updatedCount);
        verify(concernRepository, times(1)).categorizeConcernAtomically("CONCERN-002", "RCP-REVIEW-CONCERNS-001");
    }

    @Test
    @DisplayName("Given stream with concerns, When calculating coverage, Then returns correct rate and records telemetry")
    void testCalculateCoverage() {
        String stream = "reviewConcerns";
        when(concernRepository.countByStreamName(stream)).thenReturn(10L);
        when(concernRepository.countByStreamNameAndRootCausePatternIdIsNotNull(stream)).thenReturn(8L);

        CategorizationCoverageResponse response = categorizationService.calculateCoverage(stream);
        assertNotNull(response);
        assertEquals(10L, response.getTotalConcerns());
        assertEquals(8L, response.getCategorizedConcerns());
        assertEquals(80.0, response.getCoverageRate(), 0.001);

        verify(telemetryService, times(1)).recordCategorizationCoverageTelemetry(stream, 10L, 8L, 80.0);
    }

    @Test
    @DisplayName("Given non-existent recovery task ID, When resuming task, Then throws TaskNotFoundException")
    void testResumeNonExistentTask() {
        UUID nonExistentId = UUID.randomUUID();
        when(recoveryTaskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(TaskRecoveryService.TaskNotFoundException.class, () ->
                recoveryService.resumeTask(nonExistentId, "REVIVE_FAILED_TASK")
        );
    }

    @Test
    @DisplayName("Given task in non-recoverable state, When resuming task, Then throws TaskConflictException")
    void testResumeTaskNonRecoverableState() {
        UUID taskId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId, "SUB-001", "Test Task", "SETTLED", "Already settled", now, now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(TaskRecoveryService.TaskConflictException.class, () ->
                recoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK")
        );
    }

    @Test
    @DisplayName("Given valid eligible task in FAILED state, When resuming task, Then atomically updates status to IN_PROGRESS")
    void testResumeTaskSuccess() {
        UUID taskId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId, "SUB-001", "Test Task", "FAILED", "Failed due to reconcileClosedUnmergedPullRequest", now, now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask updated = recoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");
        assertNotNull(updated);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals(now, updated.getUpdatedAt());

        verify(recoveryTaskRepository, times(1)).updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", now);
    }
}
