package com.eneik.epidemiology.document;

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
import com.eneik.epidemiology.EpidemiologyApplication;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EpidemiologyApplication.class)
@AutoConfigureMockMvc
@Transactional

@WithMockUser
public class EmployeeDossierE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        employeeDocumentRepository.deleteAll();
        dossierReportRepository.deleteAll();

        EmployeeDocument doc1 = new EmployeeDocument("EMP-E2E-1", "ORDER", "Initial Order E2E", LocalDate.of(2023, 1, 1), "Content 1");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-E2E-1", "REPORT", "Initial Report E2E", LocalDate.of(2023, 2, 1), "Content 2");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-E2E-2", "EXAM", "Exam E2E", LocalDate.of(2023, 3, 1), "Content 3");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3));
    }

    @Test
    @DisplayName("Given the test suite, when the E2E tests run, then they verify that an employee can be searched and their documents displayed")
    void verifyEmployeeSearchAndDisplayDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-E2E-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Initial Report E2E"))
                .andExpect(jsonPath("$[1].title").value("Initial Order E2E"));
    }

    @Test
    @DisplayName("Given the report endpoint, when tested, then it verifies a report is generated successfully based on the initial sample content")
    void verifyReportGeneration() throws Exception {
        Map<String, Object> request = Map.of(
                "employee_id", "EMP-E2E-1",
                "template_type", "E2E_SUMMARY"
        );

        String responseContent = mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document_count").value(2))
                .andExpect(jsonPath("$.employee_id").value("EMP-E2E-1"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        Integer reportId = (Integer) responseMap.get("id");

        mockMvc.perform(get("/api/v1/dossier/reports/{id}/download", reportId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dossier_report_" + reportId + ".pdf\""))
                .andExpect(content().bytes("Сводная справка по сотруднику EMP-E2E-1: 2 документов.".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
