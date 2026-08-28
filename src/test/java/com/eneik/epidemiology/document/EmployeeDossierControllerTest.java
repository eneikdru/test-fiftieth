package com.eneik.epidemiology.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.eneik.epidemiology.telemetry.TelemetryService;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeDossierControllerTest {

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

        EmployeeDocument doc1 = new EmployeeDocument("EMP-999", "ORDER", "Приказ о назначении", LocalDate.of(2023, 1, 15), "Приказ №42");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-999", "REPORT", "Отчет по исследованию", LocalDate.of(2023, 6, 20), "Годовой отчет");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-888", "EXAM", "Экзамен", LocalDate.of(2023, 11, 10), "Оценка: отлично");
        EmployeeDocument doc4 = new EmployeeDocument("EMP-777", "Ivanov", "REPORT", "Отчет по исследованию 2", LocalDate.of(2023, 6, 20), "Годовой отчет 2");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3, doc4));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given the API contract, when a search request is made, then the backend returns the correct document list.")
    void testSearchEmployeeDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-999")
                        .param("doc_type", "ORDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Приказ о назначении"));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given an employee surname, when a search request is made, then the backend returns the documents associated with that surname.")
    void testSearchEmployeeDocumentsBySurname() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_surname", "Ivanov"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Отчет по исследованию 2"));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given a report generation request, when processed, then the backend successfully generates and returns the report file metadata.")
    void testGenerateDossierReport() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-999",
                "template_type", "SUMMARY_STANDARD"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document_count").value(2))
                .andExpect(jsonPath("$.employee_id").value("EMP-999"));

        verify(telemetryService, times(1)).recordDossierGenerationTelemetry(anyLong(), eq(true));
    }


    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given a completed report, when downloaded, then returns file content.")
    void testDownloadDossierReport() throws Exception {
        DossierReport report = new DossierReport("EMP-777", "FULL", "COMPLETED", "Test summary", 1, "/api/v1/dossier/reports/1/download");
        report = dossierReportRepository.save(report);

        mockMvc.perform(get("/api/v1/dossier/reports/{id}/download", report.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dossier_report_" + report.getId() + ".pdf\""))
                .andExpect(content().string("Test summary"));
    }
}
