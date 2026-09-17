package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.EpidemiologyApplication;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

@SpringBootTest(classes = EpidemiologyApplication.class)
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
class GqmDossierWorkflowTelemetryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        employeeDocumentRepository.deleteAll();
        dossierReportRepository.deleteAll();

        if (!userRepository.existsByUsername("user")) {
            User testUser = new User();
            testUser.setUsername("user");
            testUser.setRole("USER");
            testUser.setDepartment("Эпидемиология");
            testUser.setEmail("test@test.com");
            testUser.setFullName("Test User");
            testUser.setPasswordHash("hash");
            testUser.setCreatedAt(OffsetDateTime.now());
            userRepository.save(testUser);
        }

        EmployeeDocument doc1 = new EmployeeDocument("EMP-GQM-101", "VIROLOGY", "Report 1", LocalDate.of(2023, 1, 15), "Content 1");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-GQM-101", "EPIDEMIOLOGY", "Report 2", LocalDate.of(2023, 2, 20), "Content 2");
        employeeDocumentRepository.saveAll(List.of(doc1, doc2));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given full dossier generation workflow, When completed, Then GQM end-to-end duration metric is recorded and queryable")
    void givenFullDossierGenerationWorkflow_whenCompleted_thenGqmDurationMetricIsRecordedAndQueryable() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 9, 17, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime endTime = OffsetDateTime.of(2026, 9, 17, 10, 0, 5, 0, ZoneOffset.UTC);

        Map<String, Object> requestBody = Map.of(
                "employee_id", "EMP-GQM-101",
                "template_type", "FULL_DOSSIER",
                "session_id", "session_GQM_101",
                "session_start_time", startTime.toString(),
                "session_end_time", endTime.toString(),
                "session_duration_ms", 5000L
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated());

        // 1. Assert analysis speed GQM metric is recorded
        List<TelemetryEvent> speedEvents = telemetryEventRepository.findByEventType(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED);
        assertFalse(speedEvents.isEmpty(), "ANALYSIS_SPEED_MEASURED telemetry event must be recorded");

        TelemetryEvent speedEvent = speedEvents.stream()
                .filter(e -> "session_GQM_101".equals(e.getQueryTerm()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected telemetry event with session query term session_GQM_101"));

        assertEquals("GQM_DOSSIER_ANALYSIS_DURATION", speedEvent.getGqmMetric(), "GQM metric tag must be recorded");
        assertEquals(5000L, speedEvent.getWorkflowDurationMs(), "Workflow duration ms must match recorded metric");
        assertNotNull(speedEvent.getStartTime(), "Start time must be recorded");
        assertNotNull(speedEvent.getEndTime(), "End time must be recorded");

        // 2. Assert dossier generation metric is recorded
        List<TelemetryEvent> dossierEvents = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertFalse(dossierEvents.isEmpty(), "DOSSIER_GENERATED telemetry event must be recorded");

        TelemetryEvent dossierEvent = dossierEvents.get(0);
        assertNotNull(dossierEvent.getProcessingTimeMs(), "Processing duration ms must be non-null");
        assertTrue(dossierEvent.getProcessingTimeMs() >= 0, "Processing duration ms must be non-negative");

        // 3. Verify metrics are accessible via TelemetryService query API
        List<TelemetryEvent> queriedEvents = telemetryService.getEventsByType(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED);
        assertTrue(queriedEvents.stream().anyMatch(e -> "GQM_DOSSIER_ANALYSIS_DURATION".equals(e.getGqmMetric())),
                "Recorded GQM metric must be queryable via TelemetryService");
    }
}
