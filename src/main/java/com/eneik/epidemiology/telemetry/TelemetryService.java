package com.eneik.epidemiology.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TelemetryService {

    public static final String EVENT_ZERO_RESULTS = "ZERO_RESULTS";
    public static final String EVENT_DOWNLOAD_SUCCESS = "DOWNLOAD_SUCCESS";
    public static final String EVENT_DOSSIER_GENERATED = "DOSSIER_GENERATED";
    public static final String EVENT_DOSSIER_FAILED = "DOSSIER_FAILED";
    public static final String EVENT_CATEGORIZATION_COVERAGE_MEASURED = "CATEGORIZATION_COVERAGE_MEASURED";
    public static final String EVENT_PR_RECONCILIATION_MATCHED = "PR_RECONCILIATION_MATCHED";
    public static final String EVENT_PR_RECONCILIATION_UNMATCHED = "PR_RECONCILIATION_UNMATCHED";
    public static final String EVENT_WORKFLOW_DURATION_MEASURED = "WORKFLOW_DURATION_MEASURED";
    public static final String EVENT_ANALYSIS_SPEED_MEASURED = "ANALYSIS_SPEED_MEASURED";
    public static final String EVENT_SSO_LOGIN_SUCCESS = "sso_login_success";
    public static final String EVENT_FALLBACK_LOGIN_SUCCESS = "fallback_login_success";

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
    public TelemetryEvent recordAnalysisSpeedTelemetry(String sessionId, OffsetDateTime startTime, OffsetDateTime endTime, Long durationMs) {
        long computedDuration = (durationMs != null) ? durationMs :
                ((startTime != null && endTime != null) ? Duration.between(startTime, endTime).toMillis() : 0L);

        TelemetryEvent event = TelemetryEvent.createWorkflowEvent(
                EVENT_ANALYSIS_SPEED_MEASURED,
                sessionId,
                startTime,
                endTime,
                computedDuration,
                OffsetDateTime.now(clock)
        );
        return telemetryEventRepository.save(event);
    }

    @Transactional
    public TelemetryEvent recordReconciliationTelemetry(String prIdentifier, boolean matched) {
        TelemetryEvent event = new TelemetryEvent(
                matched ? EVENT_PR_RECONCILIATION_MATCHED : EVENT_PR_RECONCILIATION_UNMATCHED,
                prIdentifier,
                null,
                matched ? 1 : 0,
                OffsetDateTime.now(clock)
        );
        return telemetryEventRepository.save(event);
    }

    @Transactional
    public TelemetryEvent recordCategorizationCoverageTelemetry(String streamName, long totalConcerns, long categorizedConcerns, double coverageRate) {
        TelemetryEvent event = new TelemetryEvent(
                EVENT_CATEGORIZATION_COVERAGE_MEASURED,
                streamName,
                null,
                (int) categorizedConcerns,
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

    @Transactional
    public TelemetryEvent recordWorkflowTelemetry(String workflowType, OffsetDateTime startTime, OffsetDateTime endTime, Long durationMs) {
        long computedDuration = (durationMs != null) ? durationMs :
                ((startTime != null && endTime != null) ? Duration.between(startTime, endTime).toMillis() : 0L);

        TelemetryEvent event = TelemetryEvent.createWorkflowEvent(
                EVENT_WORKFLOW_DURATION_MEASURED,
                workflowType,
                startTime,
                endTime,
                computedDuration,
                OffsetDateTime.now(clock)
        );
        return telemetryEventRepository.save(event);
    }

    @Transactional
    public TelemetryEvent recordSsoLoginTelemetry(String username) {
        TelemetryEvent event = new TelemetryEvent(
                EVENT_SSO_LOGIN_SUCCESS,
                username,
                null,
                null,
                OffsetDateTime.now(clock)
        );
        return telemetryEventRepository.save(event);
    }

    @Transactional
    public TelemetryEvent recordFallbackLoginTelemetry(String username) {
        TelemetryEvent event = new TelemetryEvent(
                EVENT_FALLBACK_LOGIN_SUCCESS,
                username,
                null,
                null,
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
