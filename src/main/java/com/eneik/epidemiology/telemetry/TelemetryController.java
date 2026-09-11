package com.eneik.epidemiology.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @Autowired
    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> ingestTelemetryEvent(@RequestBody TelemetryEventDto dto) {
        if (dto == null || dto.getEventType() == null || dto.getEventType().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error_code", "VALIDATION_ERROR");
            errorResponse.put("message", "Тип события телеметрии не может быть пустым.");
            errorResponse.put("timestamp", OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        TelemetryEvent entity = dto.toEntity();
        TelemetryEvent savedEntity = telemetryService.ingestEvent(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TelemetryEventDto(savedEntity));
    }

    @GetMapping("/events")
    public ResponseEntity<List<TelemetryEventDto>> getTelemetryEvents(
            @RequestParam(value = "eventType", required = false) String eventTypeCamel,
            @RequestParam(value = "event_type", required = false) String eventTypeSnake,
            @RequestParam(value = "documentId", required = false) Long documentIdCamel,
            @RequestParam(value = "document_id", required = false) Long documentIdSnake) {

        String eventType = (eventTypeCamel != null && !eventTypeCamel.trim().isEmpty()) ? eventTypeCamel : eventTypeSnake;
        Long documentId = (documentIdCamel != null) ? documentIdCamel : documentIdSnake;

        List<TelemetryEvent> events;
        if (eventType != null && !eventType.trim().isEmpty()) {
            events = telemetryService.getEventsByType(eventType);
        } else if (documentId != null) {
            events = telemetryService.getEventsByDocumentId(documentId);
        } else {
            events = telemetryService.getAllEvents();
        }

        List<TelemetryEventDto> dtos = events.stream()
                .map(TelemetryEventDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
