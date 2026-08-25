package com.eneik.epidemiology.categorization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
