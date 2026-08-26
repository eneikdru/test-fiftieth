package com.eneik.epidemiology.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/datasets")
public class EpidemiologicalDatasetController {

    private static final Logger log = LoggerFactory.getLogger(EpidemiologicalDatasetController.class);

    private final Set<String> validDatasets = Set.of("SARS-CoV-2", "Ebola", "H1N1");

    @PostMapping("/validate")
    public ResponseEntity<?> validateDatasetInteraction(@RequestBody Map<String, Object> interaction) {
        if (interaction == null) {
            throw new IllegalArgumentException("Structural error: payload is required");
        }

        Object datasetId = interaction.get("datasetId");
        Object schemaVersion = interaction.get("schemaVersion");

        if (datasetId == null || schemaVersion == null) {
            throw new IllegalArgumentException("Structural error: datasetId and schemaVersion are required");
        }

        if (!validDatasets.contains(datasetId.toString())) {
            throw new IllegalArgumentException("Structural error: unknown datasetId");
        }

        if (!"v1.0".equals(schemaVersion.toString())) {
            throw new IllegalArgumentException("Structural error: unsupported schemaVersion");
        }

        return ResponseEntity.ok(Map.of("status", "valid"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Validation error processing epidemiological dataset interaction: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
