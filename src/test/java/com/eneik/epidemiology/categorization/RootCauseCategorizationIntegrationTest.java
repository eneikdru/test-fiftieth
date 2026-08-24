package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

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
        concern.setCreatedAt(OffsetDateTime.now());

        concernRepository.save(concern);

        int count = service.categorizeReviewConcerns("reviewConcerns");

        assertTrue(count >= 1, "At least one concern should have been categorized");

        DesignReviewConcern updated = concernRepository.findById("test-concern-epic-9").orElseThrow();
        assertEquals("RCP-REVIEW-CONCERNS-001", updated.getRootCausePatternId());
        assertEquals("CATEGORIZED", updated.getStatus());

        List<DesignReviewConcern> uncategorized = concernRepository.findByStreamNameAndRootCausePatternIdIsNull("reviewConcerns");
        assertTrue(uncategorized.isEmpty(), "No uncategorized concerns should remain");
    }
}
