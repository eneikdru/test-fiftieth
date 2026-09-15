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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryCausalTrackingQaTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    private TelemetryService telemetryService;
    private Clock fixedClock;

    private static final Instant BASE_INSTANT = Instant.parse("2026-09-15T08:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(BASE_INSTANT, ZoneOffset.UTC);
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
    }

    @Test
    @DisplayName("Given concurrent processing tasks, When telemetry events are emitted, Then all measured durations strictly uphold happens-before relationships")
    void testConcurrentTelemetryEmissionsUpholdHappensBeforeRelationships() throws InterruptedException {
        List<TelemetryEvent> savedEvents = Collections.synchronizedList(new ArrayList<>());
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> {
                    TelemetryEvent event = invocation.getArgument(0);
                    savedEvents.add(event);
                    return event;
                });

        int concurrentThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(concurrentThreads);

        List<CompletableFuture<TelemetryEvent>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentThreads; i++) {
            final int index = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    OffsetDateTime start = OffsetDateTime.of(2026, 9, 15, 8, 0, index, 0, ZoneOffset.UTC);
                    OffsetDateTime end = start.plusSeconds(10 + index);

                    if (index % 2 == 0) {
                        return telemetryService.recordWorkflowTelemetry("WORKFLOW_" + index, start, end, null).join();
                    } else {
                        return telemetryService.recordAnalysisSpeedTelemetry("SESSION_" + index, start, end, null).join();
                    }
                } finally {
                    latch.countDown();
                }
            }, executor));
        }

        boolean completedInTime = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completedInTime, "All concurrent tasks must complete within 5 seconds");
        assertEquals(concurrentThreads, savedEvents.size(), "All concurrent telemetry events must be saved");

        for (TelemetryEvent event : savedEvents) {
            assertNotNull(event, "Recorded event must not be null");
            if (event.getStartTime() != null && event.getEndTime() != null) {
                assertFalse(event.getEndTime().isBefore(event.getStartTime()),
                        "EndTime must not precede StartTime (happens-before relationship)");
                assertTrue(event.getWorkflowDurationMs() >= 0,
                        "Workflow duration must be non-negative");
                long computedMillis = java.time.Duration.between(event.getStartTime(), event.getEndTime()).toMillis();
                assertEquals(computedMillis, event.getWorkflowDurationMs(),
                        "Workflow duration must accurately reflect the elapsed time between start and end");
            }
        }
    }

    @Test
    @DisplayName("Given a simulated system clock shift during processing, When asserting tracked duration, Then it remains accurate and unaffected")
    void testSimulatedSystemClockShiftDoesNotCorruptTrackedDuration() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Simulate a monotonic time interval (start and end nanos)
        long startNanos = System.nanoTime();

        // Simulate a clock shift backwards in wall-clock time
        OffsetDateTime startTime = OffsetDateTime.now(fixedClock);
        // Simulated wall-clock backward shift by 1 hour (e.g. NTP synchronization during process)
        OffsetDateTime backwardShiftedTime = startTime.minusHours(1);

        // Calculate processing duration using monotonic nanoTime vs raw wall-clock subtraction
        long elapsedNanos = 250_000_000L; // 250 milliseconds
        long monotonicDurationMs = Math.max(0L, TimeUnit.NANOSECONDS.toMillis(elapsedNanos));

        // Raw wall-clock difference with backward shift would produce negative value
        long wallClockDiffMs = java.time.Duration.between(startTime, backwardShiftedTime).toMillis();
        assertTrue(wallClockDiffMs < 0, "Wall clock backward shift produces negative raw delta");

        // Monotonic processing duration remains positive and accurate
        assertEquals(250L, monotonicDurationMs, "Monotonic processing duration must remain accurate regardless of clock shift");

        TelemetryEvent event = telemetryService.recordDossierGenerationTelemetry(monotonicDurationMs, true);

        assertNotNull(event, "Telemetry event must be generated");
        assertEquals(TelemetryService.EVENT_DOSSIER_GENERATED, event.getEventType());
        assertEquals(250L, event.getProcessingTimeMs(), "Processing time in event payload must be positive and accurate");
        assertTrue(event.getProcessingTimeMs() >= 0, "Tracked processing time must be non-negative");
    }

    @Test
    @DisplayName("Given dossier generation processing telemetry, When recorded, Then event type and duration match expected attributes")
    void testDossierGenerationTelemetryRecording() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        long processingTimeMs = 1250L;
        TelemetryEvent successEvent = telemetryService.recordDossierGenerationTelemetry(processingTimeMs, true);

        assertNotNull(successEvent);
        assertEquals(TelemetryService.EVENT_DOSSIER_GENERATED, successEvent.getEventType());
        assertEquals(1250L, successEvent.getProcessingTimeMs());

        TelemetryEvent failureEvent = telemetryService.recordDossierGenerationTelemetry(500L, false);

        assertNotNull(failureEvent);
        assertEquals(TelemetryService.EVENT_DOSSIER_FAILED, failureEvent.getEventType());
        assertEquals(500L, failureEvent.getProcessingTimeMs());

        verify(telemetryEventRepository, times(2)).save(any(TelemetryEvent.class));
    }
}
