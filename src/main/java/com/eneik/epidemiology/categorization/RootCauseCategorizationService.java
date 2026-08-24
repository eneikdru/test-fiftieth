package com.eneik.epidemiology.categorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RootCauseCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(RootCauseCategorizationService.class);

    private final RootCausePatternRepository patternRepository;
    private final DesignReviewConcernRepository concernRepository;

    @Autowired
    public RootCauseCategorizationService(
        RootCausePatternRepository patternRepository,
        DesignReviewConcernRepository concernRepository
    ) {
        this.patternRepository = patternRepository;
        this.concernRepository = concernRepository;
    }

    @Transactional
    public int categorizeReviewConcerns(String streamName) {
        String stream = (streamName != null && !streamName.isBlank()) ? streamName : "reviewConcerns";

        RootCausePattern pattern = patternRepository.findByStreamName(stream)
            .orElseGet(() -> {
                RootCausePattern newPattern = new RootCausePattern(
                    "RCP-REVIEW-CONCERNS-001",
                    "Review Concerns Out of Control - 8 Consecutive Same Side",
                    stream,
                    "WESTERN_ELECTRIC_8_CONSECUTIVE_SAME_SIDE",
                    "RCP-REVIEW-CONCERNS-001",
                    OffsetDateTime.now()
                );
                return patternRepository.save(newPattern);
            });

        List<DesignReviewConcern> uncategorized = concernRepository.findByStreamNameAndRootCausePatternIdIsNull(stream);
        int updatedCount = 0;

        for (DesignReviewConcern concern : uncategorized) {
            int rowsUpdated = concernRepository.categorizeConcernAtomically(concern.getId(), pattern.getInvariantPatternId());
            if (rowsUpdated > 0) {
                updatedCount++;
                log.info("Atomically categorized design review concern {} with invariant rootCausePatternId {}",
                    concern.getId(), pattern.getInvariantPatternId());
            }
        }

        return updatedCount;
    }
}
