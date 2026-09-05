package com.eneik.epidemiology.telemetry;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryElapsedTimeVerificationTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    private TelemetryService telemetryService;
    private Clock fixedClock;

    private static final Instant TEST_INSTANT = Instant.parse("2026-09-05T01:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
    }

    @Test
    @DisplayName("Given simulated workflow start and end timestamps, When recordWorkflowTelemetry is called, Then elapsed time in milliseconds is accurately recorded")
    void testWorkflowTelemetryAccuratelyRecordsElapsedTime() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime startTime = OffsetDateTime.of(2026, 9, 5, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 9, 5, 0, 5, 30, 0, ZoneOffset.UTC); // 330 seconds = 330,000 ms

        TelemetryEvent event = telemetryService.recordWorkflowTelemetry("ANALYSIS_SIMULATION", startTime, endTime, null);

        assertNotNull(event, "Recorded telemetry event must not be null");
        assertEquals(TelemetryService.EVENT_WORKFLOW_DURATION_MEASURED, event.getEventType());
        assertEquals("ANALYSIS_SIMULATION", event.getQueryTerm());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(330000L, event.getWorkflowDurationMs(), "Elapsed time must accurately equal 330,000 ms");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given simulated analysis session start and end timestamps, When recordAnalysisSpeedTelemetry is called, Then elapsed time is accurately recorded")
    void testAnalysisSpeedTelemetryAccuratelyRecordsElapsedTime() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime startTime = OffsetDateTime.of(2026, 9, 5, 0, 10, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 9, 5, 0, 12, 15, 500_000_000, ZoneOffset.UTC); // 135,500 ms

        TelemetryEvent event = telemetryService.recordAnalysisSpeedTelemetry("SESS-VAL-001", startTime, endTime, null);

        assertNotNull(event, "Recorded telemetry event must not be null");
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, event.getEventType());
        assertEquals("SESS-VAL-001", event.getQueryTerm());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(135500L, event.getWorkflowDurationMs(), "Elapsed time must accurately equal 135,500 ms");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given dossier generation processing time, When recordDossierGenerationTelemetry is called, Then elapsed time in ms is recorded accurately")
    void testDossierGenerationTelemetryRecordsProcessingTime() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        long processingTimeMs = 4520L;

        TelemetryEvent event = telemetryService.recordDossierGenerationTelemetry(processingTimeMs, true);

        assertNotNull(event, "Recorded telemetry event must not be null");
        assertEquals(TelemetryService.EVENT_DOSSIER_GENERATED, event.getEventType());
        assertEquals(processingTimeMs, event.getProcessingTimeMs(), "Processing time must match recorded duration");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given explicit durationMs parameter, When recordWorkflowTelemetry is called, Then explicit elapsed duration is preserved")
    void testWorkflowTelemetryPreservesExplicitDuration() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime startTime = OffsetDateTime.of(2026, 9, 5, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 9, 5, 0, 1, 0, 0, ZoneOffset.UTC);
        long explicitDuration = 120000L;

        TelemetryEvent event = telemetryService.recordWorkflowTelemetry("PIPELINE_BATCH", startTime, endTime, explicitDuration);

        assertNotNull(event);
        assertEquals(explicitDuration, event.getWorkflowDurationMs(), "Explicit duration must take precedence");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }
}
