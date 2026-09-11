package com.eneik.epidemiology.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @Autowired
    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping({"", "/events"})
    public ResponseEntity<?> ingestTelemetry(@RequestBody TelemetryEventRequest request) {
        if (request == null || request.getEventType() == null || request.getEventType().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error_code", "INVALID_EVENT_TYPE");
            error.put("message", "Тип события обязателен.");
            error.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        TelemetryEvent event = new TelemetryEvent();
        event.setEventType(request.getEventType().trim());
        event.setQueryTerm(request.getQueryTerm());
        event.setDocumentId(request.getDocumentId());
        event.setResultsCount(request.getResultsCount());
        event.setProcessingTimeMs(request.getProcessingTimeMs());
        event.setWorkflowDurationMs(request.getWorkflowDurationMs());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setModule(request.getModule());
        event.setTitle(request.getTitle());
        event.setSuccess(request.getSuccess());
        if (request.getCreatedAt() != null) {
            event.setCreatedAt(request.getCreatedAt());
        }

        TelemetryEvent savedEvent = telemetryService.recordEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
    }

    @GetMapping({"", "/events"})
    public ResponseEntity<List<TelemetryEvent>> getTelemetryEvents(
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "documentId", required = false) Long documentId,
            @RequestParam(name = "module", required = false) String module) {

        List<TelemetryEvent> events;
        if (eventType != null && !eventType.trim().isEmpty()) {
            events = telemetryService.getEventsByType(eventType.trim());
        } else if (documentId != null) {
            events = telemetryService.getEventsByDocumentId(documentId);
        } else if (module != null && !module.trim().isEmpty()) {
            events = telemetryService.getEventsByModule(module.trim());
        } else {
            events = telemetryService.getAllEvents();
        }

        return ResponseEntity.ok(events);
    }

    @GetMapping({"/events/{id}", "/{id}"})
    public ResponseEntity<?> getTelemetryEventById(@PathVariable Long id) {
        return telemetryService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
