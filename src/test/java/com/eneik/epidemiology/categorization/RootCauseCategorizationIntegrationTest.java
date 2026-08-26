package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RootCauseCategorizationIntegrationTest {

    @Autowired
    private RootCauseCategorizationService service;

    @Autowired
    private RootCausePatternRepository patternRepository;

    @Autowired
    private DesignReviewConcernRepository concernRepository;

    @Test
    @DisplayName("Given reviewConcerns stream with uncategorized items in DB, When categorization service runs, Then assigns invariant rootCausePatternId and updates status to CATEGORIZED")
    void testEndToEndCategorizationAndAtomicUpdates() {
        // Seed test concern in DB
        DesignReviewConcern concern = new DesignReviewConcern();
        concern.setId("test-concern-epic-9");
        concern.setStreamName("reviewConcerns");
        concern.setEpicSequence(9);
        concern.setuValue(new BigDecimal("0.0000"));
        concern.setRootCausePatternId(null);
        concern.setStatus("UNCATEGORIZED");
        concern.setCreatedAt(OffsetDateTime.parse("2026-08-26T00:00:00Z"));

        concernRepository.save(concern);

        int count = service.categorizeReviewConcerns("reviewConcerns");

        assertTrue(count >= 1, "At least one concern should have been categorized");

        DesignReviewConcern updated = concernRepository.findById("test-concern-epic-9").orElseThrow();
        assertEquals("RCP-REVIEW-CONCERNS-001", updated.getRootCausePatternId());
        assertEquals("CATEGORIZED", updated.getStatus());

        List<DesignReviewConcern> uncategorized = concernRepository.findByStreamNameAndRootCausePatternIdIsNull("reviewConcerns");
        assertTrue(uncategorized.isEmpty(), "No uncategorized concerns should remain");
    }

    @Test
    @DisplayName("Given an uncategorized design review concern event from stream reviewConcerns at epic 13, When categorized in DB, Then assigns specific rootCausePatternId")
    void testEndToEndCategorizationEpic13() {
        DesignReviewConcern concern = new DesignReviewConcern();
        concern.setId("test-concern-epic-13");
        concern.setStreamName("reviewConcerns");
        concern.setEpicSequence(13);
        concern.setuValue(new BigDecimal("0.0000"));
        concern.setRootCausePatternId(null);
        concern.setStatus("UNCATEGORIZED");
        concern.setCreatedAt(OffsetDateTime.parse("2026-08-26T00:00:00Z"));

        concernRepository.save(concern);

        int count = service.categorizeReviewConcerns("reviewConcerns");

        assertTrue(count >= 1, "Epic 13 concern should have been categorized");

        DesignReviewConcern updated = concernRepository.findById("test-concern-epic-13").orElseThrow();
        assertEquals("RCP-REVIEW-CONCERNS-001", updated.getRootCausePatternId());
        assertEquals("CATEGORIZED", updated.getStatus());
    }

    @Test
    @DisplayName("Given local concern missing rootCausePatternId, When categorizeConcernInMemory is called, Then assigns rootCausePatternId in-memory")
    void testLocalInMemoryCategorization() {
        DesignReviewConcern concern = new DesignReviewConcern(
            "test-concern-epic-14", "reviewConcerns", 14, new BigDecimal("0.0000"), null, "UNCATEGORIZED", OffsetDateTime.now()
        );

        boolean categorized = service.categorizeConcernInMemory(concern);

        assertTrue(categorized);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given unsupported external schema event, When evaluateExternalSchemaEvent is called, Then logs mismatch warning and bypasses categorization")
    void testUnsupportedExternalSchemaEventEvaluation() {
        ExternalSchemaEvent unsupportedEvent = new ExternalSchemaEvent(
            "EVT-UNSUPPORTED", "reviewConcerns", "v999.0-unsupported", Map.of("epicSequence", 14)
        );

        boolean result = service.evaluateExternalSchemaEvent(unsupportedEvent);

        assertFalse(result, "Unsupported schema event should be bypassed");
    }
}
