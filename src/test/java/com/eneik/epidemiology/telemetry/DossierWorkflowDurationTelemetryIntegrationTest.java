package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.EpidemiologyApplication;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
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
@io.zonky.test.db.AutoConfigureEmbeddedDatabase(type = io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
class DossierWorkflowDurationTelemetryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryService telemetryService;

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

        if (!userRepository.existsByUsername("test_epidem_user")) {
            User testUser = new User();
            testUser.setUsername("test_epidem_user");
            testUser.setRole("USER");
            testUser.setDepartment("Epidemiology");
            testUser.setEmail("test_epidem@inst.ru");
            testUser.setFullName("Test Epidemiologist");
            testUser.setPasswordHash("hash");
            testUser.setCreatedAt(OffsetDateTime.now());
            userRepository.save(testUser);
        }

        EmployeeDocument doc = new EmployeeDocument("EMP-101", "VIROLOGY", "Analysis 1", LocalDate.of(2026, 1, 15), "Content");
        employeeDocumentRepository.save(doc);
    }

    @WithMockUser(username = "test_epidem_user", roles = "USER")
    @Test
    @DisplayName("Given a full dossier generation workflow execution, When completed, Then the end-to-end duration metric is recorded and accessible for querying")
    void testDossierWorkflowDurationTelemetryRecordedAndQueryable() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-101",
                "template_type", "ANNUAL_SUMMARY"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert duration metric is recorded in telemetry_events
        List<TelemetryEvent> eventsFromRepo = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertEquals(1, eventsFromRepo.size(), "Expected exactly one DOSSIER_GENERATED event in repository");

        TelemetryEvent recordedEvent = eventsFromRepo.get(0);
        assertNotNull(recordedEvent.getProcessingTimeMs(), "Workflow processing duration must not be null");
        assertTrue(recordedEvent.getProcessingTimeMs() >= 0, "Workflow processing duration must be non-negative");

        // Assert metric is accessible for querying via TelemetryService
        List<TelemetryEvent> eventsFromService = telemetryService.getEventsByType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertNotNull(eventsFromService, "Queried telemetry events list must not be null");
        assertEquals(1, eventsFromService.size(), "Queried telemetry events list should contain the recorded metric");
        assertEquals(recordedEvent.getId(), eventsFromService.get(0).getId());
        assertEquals(recordedEvent.getProcessingTimeMs(), eventsFromService.get(0).getProcessingTimeMs());
    }
}
