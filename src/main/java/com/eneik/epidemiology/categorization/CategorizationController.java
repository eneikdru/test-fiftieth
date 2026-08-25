package com.eneik.epidemiology.categorization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/categorization")
public class CategorizationController {

    private final RootCauseCategorizationService categorizationService;

    @Autowired
    public CategorizationController(RootCauseCategorizationService categorizationService) {
        this.categorizationService = categorizationService;
    }

    @PostMapping("/{streamName}")
    public ResponseEntity<CategorizationResult> categorizeStream(@PathVariable String streamName) {
        int count = categorizationService.categorizeReviewConcerns(streamName);
        return ResponseEntity.ok(new CategorizationResult(count));
    }

    @PostMapping("/external-event")
    public ResponseEntity<?> evaluateExternalEvent(@RequestBody ExternalSchemaEvent event) {
        boolean categorized = categorizationService.evaluateExternalSchemaEvent(event);
        return ResponseEntity.ok(Map.of("categorized", categorized));
    }
}
