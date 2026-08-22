package com.eneik.production.telemetry;

import com.eneik.production.telemetry.dto.RecordDownloadEventRequest;
import com.eneik.production.telemetry.dto.RecordZeroResultsSearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
    }

    @Test
    void recordDownloadSuccess_shouldRecordEventAndReturn201() throws Exception {
        RecordDownloadEventRequest request = new RecordDownloadEventRequest("doc-12345", "user-6789");

        mockMvc.perform(post("/api/telemetry/downloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.eventType").value("DOWNLOAD_SUCCESS"))
                .andExpect(jsonPath("$.documentId").value("doc-12345"))
                .andExpect(jsonPath("$.userId").value("user-6789"));

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryEventType.DOWNLOAD_SUCCESS);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDocumentId()).isEqualTo("doc-12345");
        assertThat(events.get(0).getUserId()).isEqualTo("user-6789");
    }

    @Test
    void recordZeroResultsSearch_shouldRecordEventAndReturn201() throws Exception {
        RecordZeroResultsSearchRequest request = new RecordZeroResultsSearchRequest("холера 2026", "user-6789");

        mockMvc.perform(post("/api/telemetry/search/zero-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.eventType").value("ZERO_RESULTS_SEARCH"))
                .andExpect(jsonPath("$.searchQuery").value("холера 2026"))
                .andExpect(jsonPath("$.userId").value("user-6789"));

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryEventType.ZERO_RESULTS_SEARCH);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSearchQuery()).isEqualTo("холера 2026");
        assertThat(events.get(0).getUserId()).isEqualTo("user-6789");
    }
}
