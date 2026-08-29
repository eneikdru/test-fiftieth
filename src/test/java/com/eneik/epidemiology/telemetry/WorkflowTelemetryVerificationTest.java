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
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowTelemetryVerificationTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    private TelemetryService telemetryService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-08-28T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
    }

    @Test
    @DisplayName("Given workflow start and end timestamps, When recordWorkflowTelemetry is called, Then duration is computed and telemetry accurately records elapsed time")
    void testRecordWorkflowTelemetryCalculatesDurationFromTimestamps() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime startTime = OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 8, 28, 10, 2, 30, 0, ZoneOffset.UTC); // 150 seconds = 150,000 ms

        TelemetryEvent event = telemetryService.recordWorkflowTelemetry("ANALYSIS_PIPELINE", startTime, endTime, null);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_WORKFLOW_DURATION_MEASURED, event.getEventType());
        assertEquals("ANALYSIS_PIPELINE", event.getQueryTerm());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(150000L, event.getWorkflowDurationMs());
        assertEquals(fixedClock.instant(), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given explicit durationMs parameter, When recordWorkflowTelemetry is called, Then pre-calculated duration is preserved in telemetry event")
    void testRecordWorkflowTelemetryPreservesExplicitDurationMs() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime startTime = OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 8, 28, 10, 1, 0, 0, ZoneOffset.UTC);

        TelemetryEvent event = telemetryService.recordWorkflowTelemetry("EPIDEMIC_MODELING", startTime, endTime, 75000L);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_WORKFLOW_DURATION_MEASURED, event.getEventType());
        assertEquals("EPIDEMIC_MODELING", event.getQueryTerm());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(75000L, event.getWorkflowDurationMs());
        assertEquals(fixedClock.instant(), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given null start and end timestamps without explicit duration, When recordWorkflowTelemetry is called, Then duration defaults to zero")
    void testRecordWorkflowTelemetryHandlesNullTimestamps() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordWorkflowTelemetry("UNKNOWN_WORKFLOW", null, null, null);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_WORKFLOW_DURATION_MEASURED, event.getEventType());
        assertEquals("UNKNOWN_WORKFLOW", event.getQueryTerm());
        assertNull(event.getStartTime());
        assertNull(event.getEndTime());
        assertEquals(0L, event.getWorkflowDurationMs());
        assertEquals(fixedClock.instant(), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }
}
