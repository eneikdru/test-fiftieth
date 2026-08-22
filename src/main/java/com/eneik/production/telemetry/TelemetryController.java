package com.eneik.production.telemetry;

import com.eneik.production.telemetry.dto.RecordDownloadEventRequest;
import com.eneik.production.telemetry.dto.RecordZeroResultsSearchRequest;
import com.eneik.production.telemetry.dto.TelemetryEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/downloads")
    public ResponseEntity<TelemetryEventResponse> recordDownloadSuccess(@Valid @RequestBody RecordDownloadEventRequest request) {
        TelemetryEventResponse response = telemetryService.recordDownloadSuccess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/search/zero-results")
    public ResponseEntity<TelemetryEventResponse> recordZeroResultsSearch(@Valid @RequestBody RecordZeroResultsSearchRequest request) {
        TelemetryEventResponse response = telemetryService.recordZeroResultsSearch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
