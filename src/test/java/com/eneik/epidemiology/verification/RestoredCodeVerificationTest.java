package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.categorization.DesignReviewConcern;
import com.eneik.epidemiology.categorization.ExternalSchemaEvent;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RestoredCodeVerificationTest {

    private RootCauseCategorizationService rootCauseCategorizationService;
    private RootCausePatternRepository patternRepository;
    private TelemetryService telemetryService;
    private TaskRecoveryService taskRecoveryService;
    private RecoveryTaskRepository recoveryTaskRepository;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        patternRepository = mock(RootCausePatternRepository.class);
        telemetryService = mock(TelemetryService.class);
        rootCauseCategorizationService = new RootCauseCategorizationService(patternRepository, null, telemetryService);

        recoveryTaskRepository = mock(RecoveryTaskRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        taskRecoveryService = new TaskRecoveryService(recoveryTaskRepository, fixedClock);
    }

    @Test
    @DisplayName("Given restored RootCauseCategorizationService, When evaluating external schema events, Then categorization completes safely")
    void testRestoredRootCauseCategorizationService() {
        ExternalSchemaEvent event = new ExternalSchemaEvent("EVT-QA-100", "reviewConcerns", "v1", Map.of("epicSequence", 14));
        boolean result = rootCauseCategorizationService.evaluateExternalSchemaEvent(event);
        assertTrue(result);

        RootCausePattern pattern = new RootCausePattern(
                "RCP-001", "Review Concerns Out of Control", "reviewConcerns",
                "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE", "RCP-REVIEW-CONCERNS-001", OffsetDateTime.now(fixedClock)
        );
        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        DesignReviewConcern concern = new DesignReviewConcern(
                "CONCERN-QA-100", "reviewConcerns", 14, new BigDecimal("0.0000"), null, "UNCATEGORIZED", OffsetDateTime.now(fixedClock)
        );
        boolean updated = rootCauseCategorizationService.categorizeConcernInMemory(concern);
        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given restored TaskRecoveryService, When resuming an eligible retired task, Then status updates atomically to IN_PROGRESS")
    void testRestoredTaskRecoveryService() {
        UUID taskId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        RecoveryTask task = new RecoveryTask(
                taskId,
                "SUB-QA-1",
                "API Slice Verification Task",
                "FAILED",
                "Failed due to reconcileClosedUnmergedPullRequest",
                now,
                now
        );

        when(recoveryTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(recoveryTaskRepository.updateStatusAtomically(eq(taskId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        RecoveryTask updated = taskRecoveryService.resumeTask(taskId, "REVIVE_FAILED_TASK");

        assertNotNull(updated);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals(now, updated.getUpdatedAt());

        verify(recoveryTaskRepository, times(1))
                .updateStatusAtomically(taskId, "FAILED", "IN_PROGRESS", now);
    }
}
