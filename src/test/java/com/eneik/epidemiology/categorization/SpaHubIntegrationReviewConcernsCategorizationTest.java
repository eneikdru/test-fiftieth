package com.eneik.epidemiology.categorization;

import com.eneik.epidemiology.telemetry.TelemetryEvent;
import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import com.eneik.epidemiology.telemetry.TelemetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SpaHubIntegrationReviewConcernsCategorizationTest {

    @Autowired
    private RootCauseCategorizationService categorizationService;

    @Autowired
    private DesignReviewConcernRepository concernRepository;

    @Autowired
    private RootCausePatternRepository patternRepository;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given SPA Hub Integration review concern at epic sequence 18, When categorization runs, Then assigns rootCausePatternId RCP-REVIEW-CONCERNS-001 and measures coverage")
    void testSpaHubIntegrationEpic18CategorizationAndCoverage() {
        DesignReviewConcern epic18Concern = new DesignReviewConcern();
        epic18Concern.setId("test-spa-hub-epic-18");
        epic18Concern.setStreamName("reviewConcerns");
        epic18Concern.setEpicSequence(18);
        epic18Concern.setuValue(new BigDecimal("0.0000"));
        epic18Concern.setRootCausePatternId(null);
        epic18Concern.setStatus("UNCATEGORIZED");
        epic18Concern.setCreatedAt(OffsetDateTime.parse("2026-08-26T14:29:59Z"));

        concernRepository.save(epic18Concern);

        int updatedCount = categorizationService.categorizeReviewConcerns("reviewConcerns");
        assertTrue(updatedCount >= 1, "At least one uncategorized concern should be updated");

        DesignReviewConcern categorized = concernRepository.findById("test-spa-hub-epic-18").orElseThrow();
        assertEquals("RCP-REVIEW-CONCERNS-001", categorized.getRootCausePatternId());
        assertEquals("CATEGORIZED", categorized.getStatus());

        CategorizationCoverageResponse coverage = categorizationService.calculateCoverage("reviewConcerns");
        assertNotNull(coverage);
        assertEquals("reviewConcerns", coverage.getStreamName());
        assertTrue(coverage.getTotalConcerns() >= 1);
        assertTrue(coverage.getCategorizedConcerns() >= 1);
        assertTrue(coverage.getCoverageRate() > 0.0 && coverage.getCoverageRate() <= 100.0);

        List<TelemetryEvent> telemetryEvents = telemetryEventRepository.findByEventType(TelemetryService.EVENT_CATEGORIZATION_COVERAGE_MEASURED);
        assertFalse(telemetryEvents.isEmpty());
    }

    @Test
    @DisplayName("Given authenticated user, When POST /api/v1/categorization/reviewConcerns and GET /api/v1/categorization/coverage, Then executes successfully")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testSpaHubIntegrationCategorizationControllerApi() throws Exception {
        DesignReviewConcern epic18Concern = new DesignReviewConcern();
        epic18Concern.setId("test-spa-hub-api-18");
        epic18Concern.setStreamName("reviewConcerns");
        epic18Concern.setEpicSequence(18);
        epic18Concern.setuValue(new BigDecimal("0.0000"));
        epic18Concern.setRootCausePatternId(null);
        epic18Concern.setStatus("UNCATEGORIZED");
        epic18Concern.setCreatedAt(OffsetDateTime.parse("2026-08-26T14:29:59Z"));

        concernRepository.save(epic18Concern);

        mockMvc.perform(post("/api/v1/categorization/reviewConcerns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorizedCount").exists());

        mockMvc.perform(get("/api/v1/categorization/coverage")
                        .param("streamName", "reviewConcerns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamName").value("reviewConcerns"))
                .andExpect(jsonPath("$.totalConcerns").exists())
                .andExpect(jsonPath("$.categorizedConcerns").exists());
    }
}
