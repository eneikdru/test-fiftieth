package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.eneik.epidemiology.telemetry.TelemetryService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RootCauseCategorizationServiceTest {

    private RootCausePatternRepository patternRepository;
    private DesignReviewConcernRepository concernRepository;
    private TelemetryService telemetryService;
    private RootCauseCategorizationService service;

    @BeforeEach
    void setUp() {
        patternRepository = mock(RootCausePatternRepository.class);
        concernRepository = mock(DesignReviewConcernRepository.class);
        telemetryService = mock(TelemetryService.class);
        service = new RootCauseCategorizationService(patternRepository, concernRepository, telemetryService);
    }

    @Test
    @DisplayName("Given uncategorized design review concerns, When categorizeReviewConcerns is called, Then assigns invariant rootCausePatternId atomically")
    void testCategorizeReviewConcerns() {
        RootCausePattern pattern = new RootCausePattern(
            "RCP-001",
            "Review Concerns Out of Control - 8 Consecutive Same Side",
            "reviewConcerns",
            "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE",
            "RCP-REVIEW-CONCERNS-001",
            OffsetDateTime.now()
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        DesignReviewConcern concern1 = new DesignReviewConcern(
            "CONCERN-1", "reviewConcerns", 9, new BigDecimal("0.0000"), null, "UNCATEGORIZED", OffsetDateTime.now()
        );
        DesignReviewConcern concern2 = new DesignReviewConcern(
            "CONCERN-2", "reviewConcerns", 10, new BigDecimal("0.0000"), null, "UNCATEGORIZED", OffsetDateTime.now()
        );

        when(concernRepository.findByStreamNameAndRootCausePatternIdIsNull("reviewConcerns"))
            .thenReturn(List.of(concern1, concern2));

        when(concernRepository.categorizeConcernAtomically("CONCERN-1", "RCP-REVIEW-CONCERNS-001")).thenReturn(1);
        when(concernRepository.categorizeConcernAtomically("CONCERN-2", "RCP-REVIEW-CONCERNS-001")).thenReturn(1);

        int count = service.categorizeReviewConcerns("reviewConcerns");

        assertEquals(2, count);
        verify(concernRepository, times(1)).categorizeConcernAtomically("CONCERN-1", "RCP-REVIEW-CONCERNS-001");
        verify(concernRepository, times(1)).categorizeConcernAtomically("CONCERN-2", "RCP-REVIEW-CONCERNS-001");
    }

    @Test
    @DisplayName("Given an uncategorized design review concern event from stream reviewConcerns at epic 9, When categorizeConcernInMemory is called, Then assigns rootCausePatternId locally and passes validation")
    void testCategorizeConcernInMemoryEpic9() {
        OffsetDateTime fixedTime = OffsetDateTime.parse("2026-08-26T00:00:00Z");
        DesignReviewConcern concern = new DesignReviewConcern(
            "DRC-EPIC-9", "reviewConcerns", 9, new BigDecimal("0.0000"), null, "UNCATEGORIZED", fixedTime
        );

        RootCausePattern pattern = new RootCausePattern(
            "RCP-001", "Review Concerns Out of Control - 8 Consecutive Same Side", "reviewConcerns",
            "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE", "RCP-REVIEW-CONCERNS-001", fixedTime
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        boolean updated = service.categorizeConcernInMemory(concern);

        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given an uncategorized design review concern event from stream reviewConcerns at epic 13, When categorizeConcernInMemory is called, Then assigns rootCausePatternId locally")
    void testCategorizeConcernInMemoryEpic13() {
        OffsetDateTime fixedTime = OffsetDateTime.parse("2026-08-26T00:00:00Z");
        DesignReviewConcern concern = new DesignReviewConcern(
            "CONCERN-EPIC-13", "reviewConcerns", 13, new BigDecimal("0.0000"), null, "UNCATEGORIZED", fixedTime
        );

        RootCausePattern pattern = new RootCausePattern(
            "RCP-001", "Review Concerns Out of Control", "reviewConcerns",
            "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE", "RCP-REVIEW-CONCERNS-001", fixedTime
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        boolean updated = service.categorizeConcernInMemory(concern);

        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given an uncategorized design review concern event from stream reviewConcerns at epic 18, When categorizeConcernInMemory is called, Then assigns rootCausePatternId locally")
    void testCategorizeConcernInMemoryEpic18() {
        OffsetDateTime fixedTime = OffsetDateTime.parse("2026-08-26T00:00:00Z");
        DesignReviewConcern concern = new DesignReviewConcern(
            "CONCERN-EPIC-18", "reviewConcerns", 18, new BigDecimal("0.0000"), null, "UNCATEGORIZED", fixedTime
        );

        RootCausePattern pattern = new RootCausePattern(
            "RCP-001", "Review Concerns Out of Control - 8 Consecutive Same Side", "reviewConcerns",
            "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE", "RCP-REVIEW-CONCERNS-001", fixedTime
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        boolean updated = service.categorizeConcernInMemory(concern);

        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given an uncategorized design review concern event from stream reviewConcerns at epic 15, When categorizeConcernInMemory is called, Then assigns rootCausePatternId locally")
    void testCategorizeConcernInMemoryEpic15() {
        OffsetDateTime fixedTime = OffsetDateTime.parse("2026-08-26T00:00:00Z");
        DesignReviewConcern concern = new DesignReviewConcern(
            "CONCERN-EPIC-15", "reviewConcerns", 15, new BigDecimal("0.0000"), null, "UNCATEGORIZED", fixedTime
        );

        RootCausePattern pattern = new RootCausePattern(
            "RCP-001", "Review Concerns Out of Control - 8 Consecutive Same Side", "reviewConcerns",
            "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE", "RCP-REVIEW-CONCERNS-001", fixedTime
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        boolean updated = service.categorizeConcernInMemory(concern);

        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given a concern missing rootCausePatternId, When categorizeConcernInMemory is called, Then assigns category in-memory")
    void testCategorizeConcernInMemory() {
        DesignReviewConcern concern = new DesignReviewConcern(
            "CONCERN-IN-MEMORY-1", "reviewConcerns", 14, new BigDecimal("0.0000"), null, "UNCATEGORIZED", OffsetDateTime.now()
        );

        RootCausePattern pattern = new RootCausePattern(
            "RCP-001", "Review Concerns Out of Control", "reviewConcerns",
            "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE", "RCP-REVIEW-CONCERNS-001", OffsetDateTime.now()
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.of(pattern));

        boolean updated = service.categorizeConcernInMemory(concern);

        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given an uncategorized concern with null stream, When categorizeConcernInMemory is called, Then assigns default invariant pattern in application code")
    void testCategorizeConcernInMemoryWithNullStream() {
        OffsetDateTime fixedTimestamp = OffsetDateTime.parse("2026-08-26T00:00:00Z");
        DesignReviewConcern concern = new DesignReviewConcern(
            "CONCERN-IN-MEMORY-NULL-STREAM", null, 12, new BigDecimal("0.0000"), null, "UNCATEGORIZED", fixedTimestamp
        );

        when(patternRepository.findByStreamName("reviewConcerns")).thenReturn(Optional.empty());

        boolean updated = service.categorizeConcernInMemory(concern);

        assertTrue(updated);
        assertEquals("RCP-REVIEW-CONCERNS-001", concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", concern.getStatus());
    }

    @Test
    @DisplayName("Given a concern already having rootCausePatternId, When categorizeConcernInMemory is called, Then skips categorization")
    void testCategorizeConcernInMemoryAlreadyCategorized() {
        DesignReviewConcern concern = new DesignReviewConcern(
            "CONCERN-IN-MEMORY-2", "reviewConcerns", 14, new BigDecimal("0.0000"), "EXISTING-RCP-ID", "CATEGORIZED", OffsetDateTime.now()
        );

        boolean updated = service.categorizeConcernInMemory(concern);

        assertFalse(updated);
        assertEquals("EXISTING-RCP-ID", concern.getRootCausePatternId());
    }

    @Test
    @DisplayName("Given a supported external schema event, When evaluateExternalSchemaEvent is called, Then evaluates successfully")
    void testEvaluateSupportedExternalSchemaEvent() {
        ExternalSchemaEvent event = new ExternalSchemaEvent(
            "EVT-001", "reviewConcerns", "v1", Map.of("epicSequence", 14)
        );

        boolean result = service.evaluateExternalSchemaEvent(event);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given out-of-control concerns, When calculateCoverage is called, Then calculates rate and records telemetry")
    void testCalculateCoverageTelemetry() {
        when(concernRepository.countByStreamName("reviewConcerns")).thenReturn(10L);
        when(concernRepository.countByStreamNameAndRootCausePatternIdIsNotNull("reviewConcerns")).thenReturn(8L);

        CategorizationCoverageResponse response = service.calculateCoverage("reviewConcerns");

        assertEquals("reviewConcerns", response.getStreamName());
        assertEquals(10L, response.getTotalConcerns());
        assertEquals(8L, response.getCategorizedConcerns());
        assertEquals(80.0, response.getCoverageRate(), 0.001);

        verify(telemetryService, times(1)).recordCategorizationCoverageTelemetry("reviewConcerns", 10L, 8L, 80.0);
    }

    @Test
    @DisplayName("Given zero out-of-control concerns, When calculateCoverage is called, Then returns 100% coverage rate without division error")
    void testCalculateCoverageZeroConcerns() {
        when(concernRepository.countByStreamName("reviewConcerns")).thenReturn(0L);
        when(concernRepository.countByStreamNameAndRootCausePatternIdIsNotNull("reviewConcerns")).thenReturn(0L);

        CategorizationCoverageResponse response = service.calculateCoverage("reviewConcerns");

        assertEquals(0L, response.getTotalConcerns());
        assertEquals(0L, response.getCategorizedConcerns());
        assertEquals(100.0, response.getCoverageRate(), 0.001);

        verify(telemetryService, times(1)).recordCategorizationCoverageTelemetry("reviewConcerns", 0L, 0L, 100.0);
    }

    @Test
    @DisplayName("Given an unsupported external schema event, When evaluateExternalSchemaEvent is called, Then logs mismatch and bypasses categorization")
    void testEvaluateUnsupportedExternalSchemaEvent() {
        ExternalSchemaEvent unsupportedVersionEvent = new ExternalSchemaEvent(
            "EVT-002", "reviewConcerns", "v99-unsupported", Map.of("epicSequence", 14)
        );

        boolean resultVersion = service.evaluateExternalSchemaEvent(unsupportedVersionEvent);
        assertFalse(resultVersion, "Should bypass categorization for unsupported schema version");

        ExternalSchemaEvent unsupportedStreamEvent = new ExternalSchemaEvent(
            "EVT-003", "unknownStream", "v1", Map.of("epicSequence", 14)
        );

        boolean resultStream = service.evaluateExternalSchemaEvent(unsupportedStreamEvent);
        assertFalse(resultStream, "Should bypass categorization for unsupported stream");
    }
}
