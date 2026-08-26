package com.eneik.epidemiology.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @Autowired
    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/operations")
    public ResponseEntity<?> recordOperationsTelemetry(@RequestBody Map<String, String> request) {
        String eventType = request.get("event_type");

        if (eventType == null || eventType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "event_type is required"));
        }

        TelemetryEvent event = telemetryService.recordOperationsTelemetry(eventType);

        if (event == null) {
             return ResponseEntity.badRequest().body(Map.of("error", "Invalid event_type"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Telemetry event successfully recorded",
            "id", event.getId()
        ));
    }
}
