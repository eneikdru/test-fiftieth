package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@TestPropertySource(properties = {
    "zonky.test.database.provider=zonky",
    "zonky.test.database.type=postgres"
})
@AutoConfigureMockMvc
@Transactional
class AnalysisSpeedTelemetryVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        dossierReportRepository.deleteAll();
        employeeDocumentRepository.deleteAll();

        EmployeeDocument doc = new EmployeeDocument(
                "EMP-ANALYSIS-01",
                "Иванов",
                "REPORT",
                "Epidemiological Analysis Document",
                LocalDate.of(2026, 8, 1),
                "EPIDEMIOLOGY"
        );
        employeeDocumentRepository.save(doc);
    }

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given active epidemiological analysis session, When user finalizes dossier with session start/end times, Then backend calculates total duration and emits ANALYSIS_SPEED_MEASURED metric")
    void testFinalizeDossierCalculatesAndEmitsAnalysisSpeedMetric() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 8, 28, 14, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 8, 28, 14, 15, 0, 0, ZoneOffset.UTC); // 15 mins = 900,000 ms

        Map<String, Object> request = Map.of(
                "employee_id", "EMP-ANALYSIS-01",
                "template_type", "SUMMARY_DOSSIER",
                "session_id", "SESSION-ANALYSIS-101",
                "session_start_time", startTime.toString(),
                "session_end_time", endTime.toString()
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED);
        assertEquals(1, events.size(), "Expected exactly one ANALYSIS_SPEED_MEASURED event");

        TelemetryEvent speedEvent = events.get(0);
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, speedEvent.getEventType());
        assertEquals("SESSION-ANALYSIS-101", speedEvent.getQueryTerm());
        assertEquals(startTime, speedEvent.getStartTime());
        assertEquals(endTime, speedEvent.getEndTime());
        assertEquals(900000L, speedEvent.getWorkflowDurationMs());
    }

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given active analysis session with explicit durationMs, When user finalizes dossier, Then ANALYSIS_SPEED_MEASURED metric captures durationMs")
    void testFinalizeDossierWithExplicitSessionDurationMs() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-ANALYSIS-01",
                "template_type", "ANALYTICS_REPORT",
                "session_id", "SESSION-ANALYSIS-202",
                "session_duration_ms", 1200000L
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED);
        assertEquals(1, events.size());

        TelemetryEvent speedEvent = events.get(0);
        assertEquals("SESSION-ANALYSIS-202", speedEvent.getQueryTerm());
        assertEquals(1200000L, speedEvent.getWorkflowDurationMs());
    }
}
