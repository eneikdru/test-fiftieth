package com.eneik.epidemiology.categorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eneik.epidemiology.telemetry.TelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class RootCauseCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(RootCauseCategorizationService.class);

    private static final Set<String> SUPPORTED_SCHEMA_VERSIONS = Set.of("v1", "1.0", "v1.0");
    private static final Set<String> SUPPORTED_STREAMS = Set.of("reviewConcerns");
    private static final String DEFAULT_PATTERN_ID = "RCP-REVIEW-CONCERNS-001";

    private final RootCausePatternRepository patternRepository;
    private final DesignReviewConcernRepository concernRepository;
    private final TelemetryService telemetryService;

    public RootCauseCategorizationService(
        RootCausePatternRepository patternRepository,
        DesignReviewConcernRepository concernRepository
    ) {
        this(patternRepository, concernRepository, null);
    }

    @Autowired
    public RootCauseCategorizationService(
        RootCausePatternRepository patternRepository,
        DesignReviewConcernRepository concernRepository,
        TelemetryService telemetryService
    ) {
        this.patternRepository = patternRepository;
        this.concernRepository = concernRepository;
        this.telemetryService = telemetryService;
    }

    public boolean categorizeConcernInMemory(DesignReviewConcern concern) {
        if (concern == null) {
            return false;
        }

        if (concern.getRootCausePatternId() != null && !concern.getRootCausePatternId().isBlank()) {
            log.info("Design review concern {} already has rootCausePatternId {}, skipping in-memory categorization",
                    concern.getId(), concern.getRootCausePatternId());
            return false;
        }

        String stream = (concern.getStreamName() != null && !concern.getStreamName().isBlank())
                ? concern.getStreamName() : "reviewConcerns";

        String patternId = patternRepository.findByStreamName(stream)
                .map(RootCausePattern::getInvariantPatternId)
                .orElse(DEFAULT_PATTERN_ID);

        concern.setRootCausePatternId(patternId);
        concern.setStatus("CATEGORIZED");
        log.info("Safely assigned rootCausePatternId {} in-memory to concern {}", patternId, concern.getId());
        return true;
    }

    public boolean evaluateExternalSchemaEvent(ExternalSchemaEvent event) {
        if (event == null) {
            log.warn("Null external schema event received, bypassing categorization");
            return false;
        }

        if (!isSupportedSchema(event)) {
            log.warn("Unsupported external schema event mismatch for eventId '{}', stream '{}', schemaVersion '{}': bypassing categorization",
                    event.getEventId(), event.getStreamName(), event.getSchemaVersion());
            return false;
        }

        log.info("Successfully evaluated external schema event '{}' for stream '{}'", event.getEventId(), event.getStreamName());
        return true;
    }

    private boolean isSupportedSchema(ExternalSchemaEvent event) {
        if (event.getStreamName() != null && !SUPPORTED_STREAMS.contains(event.getStreamName())) {
            return false;
        }

        String version = event.getSchemaVersion();
        if (version == null || version.isBlank()) {
            return true;
        }

        return SUPPORTED_SCHEMA_VERSIONS.contains(version.toLowerCase());
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

    @Transactional(readOnly = true)
    public CategorizationCoverageResponse calculateCoverage(String streamName) {
        String stream = (streamName != null && !streamName.isBlank()) ? streamName : "reviewConcerns";
        long total = concernRepository.countByStreamName(stream);
        long categorized = concernRepository.countByStreamNameAndRootCausePatternIdIsNotNull(stream);
        double coverageRate = total == 0 ? 100.0 : ((double) categorized / total) * 100.0;

        if (telemetryService != null) {
            telemetryService.recordCategorizationCoverageTelemetry(stream, total, categorized, coverageRate);
        }

        return new CategorizationCoverageResponse(stream, total, categorized, coverageRate);
    }
}
