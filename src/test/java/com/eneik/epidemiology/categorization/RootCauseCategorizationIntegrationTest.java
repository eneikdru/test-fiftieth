package com.eneik.epidemiology.categorization;

import com.eneik.epidemiology.telemetry.TelemetryEvent;
import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import com.eneik.epidemiology.telemetry.TelemetryService;
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

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Test
    @DisplayName("Given reviewConcerns stream in DB, When calculateCoverage is called, Then measures percentage of concerns with non-null rootCausePatternId and records telemetry event")
    void testCalculateCoverageIntegrationAndTelemetryPersistence() {
        DesignReviewConcern concern1 = new DesignReviewConcern(
            "test-concern-coverage-1", "reviewConcerns", 13, new BigDecimal("0.0000"), "RCP-REVIEW-CONCERNS-001", "CATEGORIZED", OffsetDateTime.now()
        );
        DesignReviewConcern concern2 = new DesignReviewConcern(
            "test-concern-coverage-2", "reviewConcerns", 14, new BigDecimal("0.0000"), null, "UNCATEGORIZED", OffsetDateTime.now()
        );

        concernRepository.saveAll(List.of(concern1, concern2));

        CategorizationCoverageResponse coverage = service.calculateCoverage("reviewConcerns");

        assertTrue(coverage.getTotalConcerns() >= 2);
        assertTrue(coverage.getCategorizedConcerns() >= 1);
        assertTrue(coverage.getCoverageRate() > 0.0 && coverage.getCoverageRate() <= 100.0);

        List<TelemetryEvent> telemetryEvents = telemetryEventRepository.findByEventType(TelemetryService.EVENT_CATEGORIZATION_COVERAGE_MEASURED);
        assertFalse(telemetryEvents.isEmpty());
        TelemetryEvent event = telemetryEvents.get(telemetryEvents.size() - 1);
        assertEquals("reviewConcerns", event.getQueryTerm());
        assertNotNull(event.getResultsCount());
        assertNotNull(event.getCreatedAt());
    }

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
    @DisplayName("Given an uncategorized design review concern event from stream reviewConcerns at epic 15, When categorized in DB, Then assigns specific rootCausePatternId")
    void testEndToEndCategorizationEpic15() {
        DesignReviewConcern concern = new DesignReviewConcern();
        concern.setId("test-concern-epic-15");
        concern.setStreamName("reviewConcerns");
        concern.setEpicSequence(15);
        concern.setuValue(new BigDecimal("0.0000"));
        concern.setRootCausePatternId(null);
        concern.setStatus("UNCATEGORIZED");
        concern.setCreatedAt(OffsetDateTime.parse("2026-08-26T00:00:00Z"));

        concernRepository.save(concern);

        int count = service.categorizeReviewConcerns("reviewConcerns");

        assertTrue(count >= 1, "Epic 15 concern should have been categorized");

        DesignReviewConcern updated = concernRepository.findById("test-concern-epic-15").orElseThrow();
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
