package com.eneik.epidemiology.telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    private TelemetryService telemetryService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-08-22T15:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
    }

    @Test
    @DisplayName("Given a search with zero results, When search telemetry is recorded, Then a ZERO_RESULTS event is stored with fixed timestamp")
    void testRecordZeroResultsSearchTelemetry() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordSearchTelemetry("эпидемия", 0);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_ZERO_RESULTS, event.getEventType());
        assertEquals("эпидемия", event.getQueryTerm());
        assertEquals(0, event.getResultsCount());
        assertNull(event.getDocumentId());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given a search with results, When search telemetry is recorded, Then no ZERO_RESULTS event is stored")
    void testRecordSearchWithResultsNoTelemetry() {
        TelemetryEvent event = telemetryService.recordSearchTelemetry("грипп", 5);

        assertNull(event);
        verify(telemetryEventRepository, never()).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given a document download action, When download telemetry is recorded, Then a DOWNLOAD_SUCCESS event is stored")
    void testRecordDownloadSuccessTelemetry() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordDownloadTelemetry(101L);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_DOWNLOAD_SUCCESS, event.getEventType());
        assertEquals(101L, event.getDocumentId());
        assertNull(event.getQueryTerm());
        assertNull(event.getResultsCount());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }
}
