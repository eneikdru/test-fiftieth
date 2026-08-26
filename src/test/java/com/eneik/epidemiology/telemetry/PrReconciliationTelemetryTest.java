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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrReconciliationTelemetryTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    private TelemetryService telemetryService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-08-26T10:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
    }

    @Test
    @DisplayName("Given a PR processed by reconciliation scanner, When matched to task, Then PR_RECONCILIATION_MATCHED telemetry is recorded")
    void testRecordReconciliationTelemetrySuccessMatch() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordReconciliationTelemetry("PR-404", true);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_MATCHED, event.getEventType());
        assertEquals("PR-404", event.getQueryTerm());
        assertEquals(1, event.getResultsCount());
        assertNull(event.getDocumentId());
        assertEquals(Instant.parse("2026-08-26T10:00:00Z"), event.getCreatedAt().toInstant());

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryEventRepository, times(1)).save(captor.capture());
        TelemetryEvent savedEvent = captor.getValue();
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_MATCHED, savedEvent.getEventType());
        assertEquals("PR-404", savedEvent.getQueryTerm());
    }

    @Test
    @DisplayName("Given a PR processed by reconciliation scanner, When unmatched to task, Then PR_RECONCILIATION_UNMATCHED telemetry is recorded")
    void testRecordReconciliationTelemetryUnmatched() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordReconciliationTelemetry("PR-405", false);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_UNMATCHED, event.getEventType());
        assertEquals("PR-405", event.getQueryTerm());
        assertEquals(0, event.getResultsCount());
        assertNull(event.getDocumentId());
        assertEquals(Instant.parse("2026-08-26T10:00:00Z"), event.getCreatedAt().toInstant());

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryEventRepository, times(1)).save(captor.capture());
        TelemetryEvent savedEvent = captor.getValue();
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_UNMATCHED, savedEvent.getEventType());
        assertEquals("PR-405", savedEvent.getQueryTerm());
    }
}
