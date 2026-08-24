package com.eneik.epidemiology.privacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RootCauseCategorizationServiceTest {

    private final RootCauseCategorizationService service = new RootCauseCategorizationService();

    @Test
    @DisplayName("Given a defect event in the stream 'reviewConcerns' representing an 8_CONSECUTIVE_SAME_SIDE shift, When the system analyzes the event, Then a specific rootCausePatternId is assigned")
    void testAssignRootCausePatternIdToReviewConcerns() {
        DefectEvent event = new DefectEvent("reviewConcerns", "8_CONSECUTIVE_SAME_SIDE");

        DefectEvent processedEvent = service.processDefectEvent(event);

        assertEquals("PATTERN_DESIGN_REVIEW_CONCERNS_INVARIANT", processedEvent.getRootCausePatternId());
    }

    @Test
    @DisplayName("Given an uncategorized defect event, When the system analyzes the event, Then rootCausePatternId remains null")
    void testUncategorizedEvent() {
        DefectEvent event = new DefectEvent("otherStream", "8_CONSECUTIVE_SAME_SIDE");

        DefectEvent processedEvent = service.processDefectEvent(event);

        assertNull(processedEvent.getRootCausePatternId());
    }
}
