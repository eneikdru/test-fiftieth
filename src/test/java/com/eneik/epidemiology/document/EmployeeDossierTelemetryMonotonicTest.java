package com.eneik.epidemiology.document;

import com.eneik.epidemiology.EpidemiologyApplication;
import com.eneik.epidemiology.telemetry.TelemetryEvent;
import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EpidemiologyApplication.class)
@AutoConfigureMockMvc
@Transactional
class EmployeeDossierTelemetryMonotonicTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

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

        if (!userRepository.existsByUsername("test_user")) {
            User user = new User();
            user.setUsername("test_user");
            user.setRole("ADMIN");
            user.setDepartment("TEST_DEP");
            user.setEmail("test@domain.com");
            user.setFullName("Test User");
            user.setPasswordHash("hash");
            user.setCreatedAt(OffsetDateTime.now());
            userRepository.save(user);
        }

        EmployeeDocument doc = new EmployeeDocument("EMP-MONO-1", "REPORT", "Test Doc", LocalDate.of(2023, 1, 1), "Content");
        employeeDocumentRepository.save(doc);
    }

    @Test
    @WithMockUser(username = "test_user", roles = "ADMIN")
    @DisplayName("Given dossier generation request, when processing completes, then telemetry records non-negative duration using monotonic source")
    void givenDossierRequest_whenGenerated_thenTelemetryRecordsNonNegativeMonotonicDuration() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-MONO-1",
                "template_type", "ANNUAL_SUMMARY"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertFalse(events.isEmpty(), "DOSSIER_GENERATED telemetry event should be present");
        TelemetryEvent event = events.get(0);
        assertNotNull(event.getProcessingTimeMs(), "Processing time must be calculated");
        assertTrue(event.getProcessingTimeMs() >= 0L, "Processing time must strictly be non-negative");
    }

    @Test
    @WithMockUser(username = "test_user", roles = "ADMIN")
    @DisplayName("Given dossier analytics export request, when processing completes, then telemetry records non-negative duration using monotonic source")
    void givenAnalyticsExportRequest_whenExported_thenTelemetryRecordsNonNegativeMonotonicDuration() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-MONO-1",
                "scientific_direction", "VIROLOGY"
        );

        mockMvc.perform(post("/api/v1/dossier/analytics/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertFalse(events.isEmpty(), "DOSSIER_GENERATED telemetry event should be present for analytics export");
        TelemetryEvent event = events.get(0);
        assertNotNull(event.getProcessingTimeMs(), "Processing time must be calculated");
        assertTrue(event.getProcessingTimeMs() >= 0L, "Processing time must strictly be non-negative");
    }
}
