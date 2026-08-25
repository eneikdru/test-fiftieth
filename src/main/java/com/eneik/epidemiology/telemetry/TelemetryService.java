package com.eneik.epidemiology.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TelemetryService {

    public static final String EVENT_ZERO_RESULTS = "ZERO_RESULTS";
    public static final String EVENT_DOWNLOAD_SUCCESS = "DOWNLOAD_SUCCESS";
    public static final String EVENT_DOSSIER_GENERATED = "DOSSIER_GENERATED";
    public static final String EVENT_DOSSIER_FAILED = "DOSSIER_FAILED";

    private final TelemetryEventRepository telemetryEventRepository;
    private final Clock clock;

    @Autowired
    public TelemetryService(TelemetryEventRepository telemetryEventRepository) {
        this(telemetryEventRepository, Clock.systemUTC());
    }

    public TelemetryService(TelemetryEventRepository telemetryEventRepository, Clock clock) {
        this.telemetryEventRepository = telemetryEventRepository;
        this.clock = clock;
    }

    @Transactional
    public TelemetryEvent recordSearchTelemetry(String queryTerm, int resultsCount) {
        if (resultsCount == 0) {
            TelemetryEvent event = new TelemetryEvent(
                    EVENT_ZERO_RESULTS,
                    queryTerm,
                    null,
                    resultsCount,
                    OffsetDateTime.now(clock)
            );
            return telemetryEventRepository.save(event);
        }
        return null;
    }

    @Transactional
    public TelemetryEvent recordDownloadTelemetry(Long documentId) {
        TelemetryEvent event = new TelemetryEvent(
                EVENT_DOWNLOAD_SUCCESS,
                null,
                documentId,
                null,
                OffsetDateTime.now(clock)
        );
        return telemetryEventRepository.save(event);
    }

    @Transactional
    public TelemetryEvent recordDossierGenerationTelemetry(long processingTimeMs, boolean success) {
        TelemetryEvent event = new TelemetryEvent(
                success ? EVENT_DOSSIER_GENERATED : EVENT_DOSSIER_FAILED,
                null,
                null,
                null,
                processingTimeMs,
                OffsetDateTime.now(clock)
        );
        return telemetryEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<TelemetryEvent> getEventsByType(String eventType) {
        return telemetryEventRepository.findByEventType(eventType);
    }

    @Transactional(readOnly = true)
    public List<TelemetryEvent> getEventsByDocumentId(Long documentId) {
        return telemetryEventRepository.findByDocumentId(documentId);
    }
}
