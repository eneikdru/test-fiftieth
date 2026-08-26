package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RootCauseCategorizationService categorizationService;

    @Test
    @DisplayName("Given an authenticated user, When GET /api/v1/categorization/coverage, Then returns categorization coverage metric")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetCategorizationCoverage() throws Exception {
        CategorizationCoverageResponse coverage = new CategorizationCoverageResponse("reviewConcerns", 10L, 8L, 80.0);
        when(categorizationService.calculateCoverage("reviewConcerns")).thenReturn(coverage);

        mockMvc.perform(get("/api/v1/categorization/coverage")
                        .param("streamName", "reviewConcerns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamName").value("reviewConcerns"))
                .andExpect(jsonPath("$.totalConcerns").value(10))
                .andExpect(jsonPath("$.categorizedConcerns").value(8))
                .andExpect(jsonPath("$.coverageRate").value(80.0));

        verify(categorizationService, times(1)).calculateCoverage("reviewConcerns");
    }

    @Test
    @DisplayName("Given an authenticated user, When POST /api/v1/categorization/{streamName}, Then invokes service and returns categorized count")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCategorizeStream() throws Exception {
        when(categorizationService.categorizeReviewConcerns("reviewConcerns")).thenReturn(13);

        mockMvc.perform(post("/api/v1/categorization/reviewConcerns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorizedCount").value(13));

        verify(categorizationService, times(1)).categorizeReviewConcerns("reviewConcerns");
    }

    @Test
    @DisplayName("Given an unauthenticated user, When POST /api/v1/categorization/{streamName}, Then returns 401")
    void testCategorizeStreamUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/categorization/reviewConcerns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given an authenticated user and external event, When POST /api/v1/categorization/external-event, Then returns categorization status")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testEvaluateExternalEvent() throws Exception {
        when(categorizationService.evaluateExternalSchemaEvent(any())).thenReturn(true);

        String payload = """
                {
                    "eventId": "EVT-100",
                    "streamName": "reviewConcerns",
                    "schemaVersion": "v1",
                    "payload": {"epicSequence": 14}
                }
                """;

        mockMvc.perform(post("/api/v1/categorization/external-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized").value(true));

        verify(categorizationService, times(1)).evaluateExternalSchemaEvent(any());
    }
}
