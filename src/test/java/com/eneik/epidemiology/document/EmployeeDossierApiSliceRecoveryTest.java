package com.eneik.epidemiology.document;

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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeDossierApiSliceRecoveryTest {

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

        EmployeeDocument document = new EmployeeDocument(
                "EMP-202",
                "REPORT",
                "Эпидемиологический отчет за первый квартал",
                LocalDate.of(2026, 3, 15),
                "Квартальный отчет"
        );
        document.setScientificDirection("EPIDEMIOLOGY");
        document.setEmployeeSurname("Иванов");
        employeeDocumentRepository.save(document);
    }

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given valid employee search query, When searching documents, Then returns matching document payload accurately")
    void testSearchEmployeeDocuments_Success() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-202")
                        .param("employee_surname", "Иванов")
                        .param("scientific_direction", "EPIDEMIOLOGY"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].employee_id").value("EMP-202"))
                .andExpect(jsonPath("$.results[0].employee_surname").value("Иванов"))
                .andExpect(jsonPath("$.results[0].scientific_direction").value("EPIDEMIOLOGY"));
    }

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given valid report generation payload, When requested, Then creates dossier report and returns status 201 Created")
    void testGenerateDossierReport_Success() throws Exception {
        Map<String, Object> payload = Map.of(
                "employee_id", "EMP-202",
                "template_type", "SUMMARY_DETAILED"
        );

        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee_id").value("EMP-202"))
                .andExpect(jsonPath("$.template_type").value("SUMMARY_DETAILED"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document_count").value(1));
    }
}
