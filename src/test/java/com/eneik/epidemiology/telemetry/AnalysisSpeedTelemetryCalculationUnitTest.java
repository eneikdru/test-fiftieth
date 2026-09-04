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
class AnalysisSpeedTelemetryCalculationUnitTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    private TelemetryService telemetryService;
    private Clock fixedClock;

    private static final Instant TEST_INSTANT = Instant.parse("2026-08-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
    }

    @Test
    @DisplayName("Given workflow start and end timestamps, When recordAnalysisSpeedTelemetry is called, Then duration is accurately computed and emitted in payload")
    void testDurationCalculationFromTimestamps() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime start = OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 8, 28, 10, 15, 30, 0, ZoneOffset.UTC); // 15 min 30 sec = 930,000 ms

        TelemetryEvent event = telemetryService.recordAnalysisSpeedTelemetry("ANALYSIS-SESS-100", start, end, null);

        assertNotNull(event, "Emitted telemetry event must not be null");
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, event.getEventType(), "Event type must match ANALYSIS_SPEED_MEASURED");
        assertEquals("ANALYSIS-SESS-100", event.getQueryTerm(), "Query term field should contain the analysis session ID");
        assertEquals(start, event.getStartTime(), "Start time in payload must match provided start timestamp");
        assertEquals(end, event.getEndTime(), "End time in payload must match provided end timestamp");
        assertEquals(930000L, event.getWorkflowDurationMs(), "Calculated duration must equal 930,000 ms");
        assertEquals(OffsetDateTime.now(fixedClock), event.getCreatedAt(), "Creation timestamp must match fixed clock time");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given explicit durationMs, When recordAnalysisSpeedTelemetry is called, Then explicit duration is prioritized and emitted")
    void testExplicitDurationMsTakesPrecedence() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime start = OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 8, 28, 10, 20, 0, 0, ZoneOffset.UTC);

        TelemetryEvent event = telemetryService.recordAnalysisSpeedTelemetry("ANALYSIS-SESS-200", start, end, 500000L);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, event.getEventType());
        assertEquals("ANALYSIS-SESS-200", event.getQueryTerm());
        assertEquals(500000L, event.getWorkflowDurationMs(), "Explicit durationMs must be preserved");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given null timestamps and null duration, When recordAnalysisSpeedTelemetry is called, Then default 0 duration is emitted safely")
    void testFallbackForNullTimestamps() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordAnalysisSpeedTelemetry("ANALYSIS-SESS-300", null, null, null);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, event.getEventType());
        assertEquals("ANALYSIS-SESS-300", event.getQueryTerm());
        assertNull(event.getStartTime());
        assertNull(event.getEndTime());
        assertEquals(0L, event.getWorkflowDurationMs(), "Duration should fall back to 0L when inputs are missing");

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }
}
