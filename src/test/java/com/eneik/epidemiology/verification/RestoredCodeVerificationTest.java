package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.categorization.DesignReviewConcern;
import com.eneik.epidemiology.categorization.ExternalSchemaEvent;
import com.eneik.epidemiology.categorization.RootCauseCategorizationService;
import com.eneik.epidemiology.categorization.RootCausePattern;
import com.eneik.epidemiology.categorization.RootCausePatternRepository;
import com.eneik.epidemiology.process.BackgroundProcess;
import com.eneik.epidemiology.process.BackgroundProcessRepository;
import com.eneik.epidemiology.process.ProcessRecoveryService;
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
    private ProcessRecoveryService processRecoveryService;
    private BackgroundProcessRepository recoveryProcessRepository;
    private Clock fixedClock;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        patternRepository = mock(RootCausePatternRepository.class);
        telemetryService = mock(TelemetryService.class);
        rootCauseCategorizationService = new RootCauseCategorizationService(patternRepository, null, telemetryService);

        recoveryProcessRepository = mock(BackgroundProcessRepository.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        processRecoveryService = new ProcessRecoveryService(recoveryProcessRepository, fixedClock);
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
    @DisplayName("Given restored ProcessRecoveryService, When resuming an eligible retired process, Then status updates atomically to IN_PROGRESS")
    void testRestoredProcessRecoveryService() {
        UUID processId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(fixedClock);
        BackgroundProcess process = new BackgroundProcess(
                processId,
                "SUB-QA-1",
                "API Slice Verification Process",
                "FAILED",
                "Failed due to data_processing_error",
                now,
                now
        );

        when(recoveryProcessRepository.findById(processId)).thenReturn(Optional.of(process));
        when(recoveryProcessRepository.updateStatusAtomically(eq(processId), eq("FAILED"), eq("IN_PROGRESS"), any(OffsetDateTime.class)))
                .thenReturn(1);

        BackgroundProcess updated = processRecoveryService.resumeProcess(processId, "REVIVE_FAILED_TASK");

        assertNotNull(updated);
        assertEquals("IN_PROGRESS", updated.getStatus());
        assertEquals(now, updated.getUpdatedAt());

        verify(recoveryProcessRepository, times(1))
                .updateStatusAtomically(processId, "FAILED", "IN_PROGRESS", now);
    }
}
