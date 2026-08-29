package com.eneik.epidemiology.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EpidemiologicalSurveillanceVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("Given the seeded database, When epidemiological search and export flow runs, Then search succeeds and report export completes")
    void testEpidemiologicalSearchAndExportFlow() throws Exception {
        // Step 1: Epidemiological search for documents against seeded surveillance reports
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "гриппу"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(1)));

        // Step 2: Trigger analytics PDF report export for employee dossier/surveillance
        Map<String, Object> exportRequest = Map.of(
                "employee_id", "EMP-001",
                "doc_types", List.of("REPORT", "ORDER")
        );

        mockMvc.perform(post("/api/v1/dossier/analytics/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.export_id").isNumber())
                .andExpect(jsonPath("$.download_url").value(containsString("/api/v1/dossier/reports/")));
    }

    @Test
    @WithMockUser
    @DisplayName("Given the seeded database, When surveillance authority KPI flow runs, Then metrics endpoint returns denominator and confidence bounds end-to-end")
    void testSurveillanceAuthorityKpiFlow() throws Exception {
        // Query surveillance authority KPI metrics for an employee
        mockMvc.perform(get("/api/v1/dossier/analytics/metrics")
                        .param("employee_id", "EMP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_id").value("EMP-001"))
                .andExpect(jsonPath("$.metric_name").value("Доля научных отчетов в общем объеме документов"))
                .andExpect(jsonPath("$.value").isNumber())
                .andExpect(jsonPath("$.denominator").isNumber())
                .andExpect(jsonPath("$.lower_bound").isNumber())
                .andExpect(jsonPath("$.upper_bound").isNumber());
    }
}
