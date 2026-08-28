package com.eneik.epidemiology.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.eneik.epidemiology.telemetry.TelemetryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class EmployeeDossierApiSliceVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @MockBean
    private TelemetryService telemetryService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        employeeDocumentRepository.deleteAll();
        dossierReportRepository.deleteAll();

        EmployeeDocument doc1 = new EmployeeDocument("EMP-101", "ORDER", "Приказ о назначении исследователем", LocalDate.of(2024, 1, 10), "Приказ №101/К");
        doc1.setScientificDirection("EPIDEMIOLOGY");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-101", "REPORT", "Годовой эпидемиологический отчет", LocalDate.of(2024, 5, 12), "Итоговый отчет 2024");
        doc2.setScientificDirection("EPIDEMIOLOGY");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given the API endpoint is called with a concrete test payload representing employee document data, When the request is valid, Then it returns a 200 OK response with the correct document format.")
    void testGetEmployeeDocumentsSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-101")
                        .param("doc_type", "ORDER"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employee_id").value("EMP-101"))
                .andExpect(jsonPath("$[0].doc_type").value("ORDER"))
                .andExpect(jsonPath("$[0].title").value("Приказ о назначении исследователем"))
                .andExpect(jsonPath("$[0].details").value("Приказ №101/К"));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given an invalid request payload for report generation, When the endpoint is called, Then it returns a 400 Bad Request.")
    void testGenerateReportInvalidPayload() throws Exception {
        // Missing mandatory parameter 'employee_id'
        Map<String, Object> invalidRequest = Map.of(
                "template_type", "SUMMARY_STANDARD"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", notNullValue()));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given valid report generation payload, When POST is sent, Then returns 201 Created with DossierReport response.")
    void testGenerateReportSuccess() throws Exception {
        Map<String, Object> validRequest = Map.of(
                "employee_id", "EMP-101",
                "template_type", "SUMMARY_STANDARD"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee_id").value("EMP-101"))
                .andExpect(jsonPath("$.template_type").value("SUMMARY_STANDARD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document_count").value(2));
    }
}
