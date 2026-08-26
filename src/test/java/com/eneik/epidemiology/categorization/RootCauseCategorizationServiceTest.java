package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RootCauseCategorizationServiceTest {

    private RootCausePatternRepository patternRepository;
    private DesignReviewConcernRepository concernRepository;
    private RootCauseCategorizationService service;

    @BeforeEach
    void setUp() {
        patternRepository = mock(RootCausePatternRepository.class);
        concernRepository = mock(DesignReviewConcernRepository.class);
        service = new RootCauseCategorizationService(patternRepository, concernRepository);
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
