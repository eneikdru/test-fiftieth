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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    @Test
    @DisplayName("Given a dossier generation, When generation telemetry is recorded, Then a DOSSIER_GENERATED event is stored with processing time")
    void testRecordDossierGenerationTelemetry() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordDossierGenerationTelemetry(1500L, true);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_DOSSIER_GENERATED, event.getEventType());
        assertEquals(1500L, event.getProcessingTimeMs());
        assertNull(event.getDocumentId());
        assertNull(event.getQueryTerm());
        assertNull(event.getResultsCount());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given a failed dossier generation, When generation telemetry is recorded, Then a DOSSIER_FAILED event is stored with processing time")
    void testRecordDossierGenerationFailureTelemetry() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordDossierGenerationTelemetry(300L, false);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_DOSSIER_FAILED, event.getEventType());
        assertEquals(300L, event.getProcessingTimeMs());
        assertNull(event.getDocumentId());
        assertNull(event.getQueryTerm());
        assertNull(event.getResultsCount());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given a matched PR reconciliation, When telemetry is recorded, Then PR_RECONCILIATION_MATCHED event is stored with count 1")
    void testRecordReconciliationTelemetryMatched() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordReconciliationTelemetry("PR-101", true);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_MATCHED, event.getEventType());
        assertEquals("PR-101", event.getQueryTerm());
        assertEquals(1, event.getResultsCount());
        assertNull(event.getDocumentId());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given an unmatched PR reconciliation, When telemetry is recorded, Then PR_RECONCILIATION_UNMATCHED event is stored with count 0")
    void testRecordReconciliationTelemetryUnmatched() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordReconciliationTelemetry("PR-102", false);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_UNMATCHED, event.getEventType());
        assertEquals("PR-102", event.getQueryTerm());
        assertEquals(0, event.getResultsCount());
        assertNull(event.getDocumentId());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given workflow execution times, When recordWorkflowTelemetry is called, Then WORKFLOW_DURATION_MEASURED event is saved with start/end times and calculated elapsed duration")
    void testRecordWorkflowTelemetry() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime startTime = OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 8, 28, 10, 0, 5, 0, ZoneOffset.UTC);

        TelemetryEvent event = telemetryService.recordWorkflowTelemetry("DOSSIER_ANALYSIS", startTime, endTime, null);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_WORKFLOW_DURATION_MEASURED, event.getEventType());
        assertEquals("DOSSIER_ANALYSIS", event.getQueryTerm());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(5000L, event.getWorkflowDurationMs());
        assertEquals(Instant.parse("2026-08-22T15:00:00Z"), event.getCreatedAt().toInstant());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }
}
