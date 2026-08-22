package com.eneik.production.telemetry;

import com.eneik.production.telemetry.dto.RecordDownloadEventRequest;
import com.eneik.production.telemetry.dto.RecordZeroResultsSearchRequest;
import com.eneik.production.telemetry.dto.TelemetryEventResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class TelemetryService {

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
    public TelemetryEventResponse recordDownloadSuccess(RecordDownloadEventRequest request) {
        TelemetryEvent event = new TelemetryEvent(
                TelemetryEventType.DOWNLOAD_SUCCESS,
                request.getDocumentId(),
                null,
                request.getUserId(),
                OffsetDateTime.now(clock)
        );
        TelemetryEvent saved = telemetryEventRepository.save(event);
        return mapToResponse(saved);
    }

    @Transactional
    public TelemetryEventResponse recordZeroResultsSearch(RecordZeroResultsSearchRequest request) {
        TelemetryEvent event = new TelemetryEvent(
                TelemetryEventType.ZERO_RESULTS_SEARCH,
                null,
                request.getSearchQuery(),
                request.getUserId(),
                OffsetDateTime.now(clock)
        );
        TelemetryEvent saved = telemetryEventRepository.save(event);
        return mapToResponse(saved);
    }

    private TelemetryEventResponse mapToResponse(TelemetryEvent event) {
        return new TelemetryEventResponse(
                event.getId(),
                event.getEventType(),
                event.getDocumentId(),
                event.getSearchQuery(),
                event.getUserId(),
                event.getCreatedAt()
        );
    }
}
