package com.eneik.epidemiology.document;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import com.eneik.epidemiology.telemetry.TelemetryService;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.util.List;
import static org.hamcrest.Matchers.closeTo;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeDossierAnalyticsControllerTest {

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

        EmployeeDocument doc1 = new EmployeeDocument("EMP-123", "Ivanov", "REPORT", "Отчет 1", LocalDate.of(2023, 1, 15), "Детали 1", "VIRUSOLOGY");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-123", "Ivanov", "ORDER", "Приказ 1", LocalDate.of(2023, 5, 20), "Детали 2", "VIRUSOLOGY");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-123", "Ivanov", "REPORT", "Отчет 2", LocalDate.of(2023, 10, 10), "Детали 3", "BACTERIOLOGY");
        EmployeeDocument doc4 = new EmployeeDocument("EMP-456", "Petrov", "REPORT", "Отчет 3", LocalDate.of(2023, 6, 20), "Детали 4", "VIRUSOLOGY");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3, doc4));
    }

    @Test
    @WithMockUser
    @DisplayName("Given valid parameters, when calling /documents, then returns filtered documents")
    void testFilterAnalyticsDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/analytics/documents")
                        .param("employee_id", "EMP-123")
                        .param("scientific_direction", "VIRUSOLOGY")
                        .param("doc_type", "REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Отчет 1"))
                .andExpect(jsonPath("$[0].doc_type").value("REPORT"));
    }

    @Test
    @WithMockUser
    @DisplayName("Given valid parameters, when calling /export, then simulates export and returns status")
    void testExportAnalyticsPdf() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-123",
                "scientific_direction", "VIRUSOLOGY",
                "doc_types", List.of("REPORT", "ORDER")
        );

        mockMvc.perform(post("/api/v1/dossier/analytics/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.export_id").isNumber());

        verify(telemetryService, times(1)).recordDossierGenerationTelemetry(anyLong(), eq(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Given valid parameters, when calling /metrics, then returns calculated metrics")
    void testGetAnalyticsMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/analytics/metrics")
                        .param("employee_id", "EMP-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_id").value("EMP-123"))
                .andExpect(jsonPath("$.metric_name").value("Доля научных отчетов в общем объеме документов"))
                .andExpect(jsonPath("$.denominator").value(3)) // Total docs for EMP-123
                .andExpect(jsonPath("$.value").value(org.hamcrest.Matchers.closeTo(2.0/3.0, 0.0001))); // 2 REPORTS out of 3 docs
    }
    @Test
    @WithMockUser
    @DisplayName("Given valid parameters, when calling /reports/{id}/download, then simulates PDF download")
    void testDownloadAnalyticsReport() throws Exception {
        DossierReport report = new DossierReport("EMP-123", "ANALYTICS_EXPORT", "COMPLETED", "Test report summary", 1, "/api/v1/dossier/reports/99/download");
        report = dossierReportRepository.save(report);

        mockMvc.perform(get("/api/v1/dossier/analytics/reports/{id}/download", report.getId()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dossier_report_" + report.getId() + ".pdf\""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(org.springframework.http.MediaType.APPLICATION_PDF));
        // Removing the exact string content check because it is now a generated PDF byte stream.
    }
}
