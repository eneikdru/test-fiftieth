package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.EpidemiologyApplication;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EpidemiologyApplication.class)
@AutoConfigureMockMvc
@Transactional

@WithMockUser
class DossierTelemetryVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        employeeDocumentRepository.deleteAll();
        dossierReportRepository.deleteAll();

        EmployeeDocument doc1 = new EmployeeDocument("EMP-TEL-100", "VIROLOGY", "Order 1", LocalDate.of(2023, 5, 10), "Content 1");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-TEL-100", "BACTERIOLOGY", "Report 2", LocalDate.of(2023, 6, 12), "Content 2");
        employeeDocumentRepository.saveAll(List.of(doc1, doc2));
    }

    @Test
    @DisplayName("Given valid dossier report request, When report generation executes, Then DOSSIER_GENERATED telemetry metric is recorded with non-negative processing time")
    void givenValidDossierRequest_whenExecuted_thenDossierGeneratedMetricIsRecorded() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-TEL-100",
                "template_type", "ANNUAL_SUMMARY"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertEquals(1, events.size(), "Expected exactly one DOSSIER_GENERATED telemetry event");

        TelemetryEvent event = events.get(0);
        assertEquals(TelemetryService.EVENT_DOSSIER_GENERATED, event.getEventType());
        assertNotNull(event.getProcessingTimeMs(), "Processing time must not be null");
        assertTrue(event.getProcessingTimeMs() >= 0, "Processing time must be non-negative");
        assertNotNull(event.getCreatedAt(), "Creation timestamp must be present");
    }

    @Test
    @DisplayName("Given dossier request targeting specific documents, When report generation executes, Then telemetry metric is persisted correctly")
    void givenDossierRequestWithDocumentIds_whenExecuted_thenTelemetryMetricIsPersisted() throws Exception {
        EmployeeDocument doc = employeeDocumentRepository.findAll().get(0);

        Map<String, Object> request = Map.of(
                "employee_id", "EMP-TEL-100",
                "template_type", "SPECIFIC_DOCS",
                "document_ids", List.of(doc.getId())
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        assertEquals(1, events.size(), "Expected exactly one DOSSIER_GENERATED event for filtered report");
        assertNotNull(events.get(0).getCreatedAt());
    }

    @Test
    @DisplayName("Given invalid request missing mandatory fields, When validation fails, Then no dossier telemetry event is recorded")
    void givenInvalidDossierRequest_whenValidationFails_thenNoDossierTelemetryRecorded() throws Exception {
        Map<String, Object> invalidRequest = Map.of(
                "template_type", "SUMMARY_ONLY"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        List<TelemetryEvent> generatedEvents = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_GENERATED);
        List<TelemetryEvent> failedEvents = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOSSIER_FAILED);

        assertTrue(generatedEvents.isEmpty(), "No DOSSIER_GENERATED event should be recorded on validation failure");
        assertTrue(failedEvents.isEmpty(), "No DOSSIER_FAILED event should be recorded when validation blocks request processing");
    }
}
