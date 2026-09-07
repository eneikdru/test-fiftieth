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
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import java.util.Optional;
import static org.mockito.Mockito.when;
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
    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        employeeDocumentRepository.deleteAll();
        dossierReportRepository.deleteAll();

        EmployeeDocument doc1 = new EmployeeDocument("EMP-999", "ORDER", "Приказ о назначении", LocalDate.of(2023, 1, 15), "Приказ №42");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-999", "REPORT", "Отчет по исследованию", LocalDate.of(2023, 6, 20), "Годовой отчет");
        doc2.setAccessDepartment("Эпидемиология");
        EmployeeDocument doc3 = new EmployeeDocument("EMP-888", "EXAM", "Экзамен", LocalDate.of(2023, 11, 10), "Оценка: отлично");
        EmployeeDocument doc4 = new EmployeeDocument("EMP-777", "Ivanov", "REPORT", "Отчет по исследованию 2", LocalDate.of(2023, 6, 20), "Годовой отчет 2");
        doc4.setAccessDepartment("Эпидемиология");

        employeeDocumentRepository.saveAll(List.of(doc1, doc2, doc3, doc4));

        User testUser = new User();
        testUser.setUsername("user");
        testUser.setRole("USER");
        testUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(testUser));

        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setRole("USER");
        otherUser.setDepartment("Вирусология");
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
    }
    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given valid reports, when paginated list requested, then return correct page.")
    void testListDossierReports() throws Exception {
        DossierReport report1 = new DossierReport("EMP-777", "FULL", "COMPLETED", "Test summary", 1, "/api/v1/dossier/reports/1/download");
        report1.setAccessDepartment("Эпидемиология");
        DossierReport report2 = new DossierReport("EMP-888", "FULL", "COMPLETED", "Test summary", 1, "/api/v1/dossier/reports/2/download");
        report2.setAccessDepartment("Вирусология");

        dossierReportRepository.saveAll(List.of(report1, report2));

        mockMvc.perform(get("/api/v1/dossier/reports")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employee_id").value("EMP-777"));
    }




    @WithMockUser(username = "other", roles = "USER")
    @Test
    @DisplayName("Given a mismatched department, when a search request is made, then restricted documents are filtered out.")
    void testSearchEmployeeDocumentsAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only ORDER is visible, REPORT is filtered
                .andExpect(jsonPath("$[0].title").value("Приказ о назначении"));
    }

    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given a matching department, when a search request is made, then restricted documents are included.")
    void testSearchEmployeeDocumentsAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_id", "EMP-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @WithMockUser(username = "user", roles = "USER")
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


    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given an employee surname, when a search request is made, then the backend returns the documents associated with that surname.")
    void testSearchEmployeeDocumentsBySurname() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/documents")
                        .param("employee_surname", "Ivanov"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Отчет по исследованию 2"));
    }


    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given a report generation request, when processed, then the backend successfully generates and returns the report file metadata and sets access fields.")
    void testGenerateDossierReport() throws Exception {
        User userWithCourses = new User();
        userWithCourses.setUsername("user");
        userWithCourses.setRole("USER");
        userWithCourses.setDepartment("Эпидемиология");
        userWithCourses.setCourses("EPID-101, EPID-102");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(userWithCourses));

        Map<String, Object> request = Map.of(
                "employee_id", "EMP-999",
                "template_type", "SUMMARY_STANDARD"
        );

        String responseJson = mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document_count").value(2))
                .andExpect(jsonPath("$.employee_id").value("EMP-999"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        Long reportId = ((Number) responseMap.get("id")).longValue();

        DossierReport savedReport = dossierReportRepository.findById(reportId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Эпидемиология", savedReport.getAccessDepartment());
        org.junit.jupiter.api.Assertions.assertEquals("EPID-101, EPID-102", savedReport.getAccessCourse());

        verify(telemetryService, times(1)).recordDossierGenerationTelemetry(anyLong(), eq(true));
    }


    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given a completed report, when downloaded, then returns multi-page PDF document.")
    void testDownloadDossierReport() throws Exception {
        DossierReport report = new DossierReport("EMP-999", "FULL", "COMPLETED", "Test summary", 2, "/api/v1/dossier/reports/1/download");
        report = dossierReportRepository.save(report);

        byte[] pdfBytes = mockMvc.perform(get("/api/v1/dossier/reports/{id}/download", report.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dossier_report_" + report.getId() + ".pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        com.itextpdf.text.pdf.PdfReader reader = new com.itextpdf.text.pdf.PdfReader(pdfBytes);
        org.junit.jupiter.api.Assertions.assertTrue(reader.getNumberOfPages() >= 2);
    }

    @Test
    @DisplayName("Given unauthenticated user, When accessing dossier reports, Then returns 401 Unauthorized.")
    void testGetDossierReportUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/dossier/reports/1"))
                .andExpect(status().isUnauthorized());
    }

    @WithMockUser(username = "other", roles = "USER")
    @Test
    @DisplayName("Given unauthorized department user, When accessing restricted report, Then returns 403 Forbidden.")
    void testGetDossierReportForbidden() throws Exception {
        DossierReport report = new DossierReport("EMP-777", "FULL", "COMPLETED", "Restricted summary", 1, null);
        report.setAccessDepartment("Эпидемиология");
        report = dossierReportRepository.save(report);

        mockMvc.perform(get("/api/v1/dossier/reports/{id}", report.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("FORBIDDEN"));
    }

    @WithMockUser(username = "user", roles = "USER")
    @Test
    @DisplayName("Given missing mandatory parameters, When generating dossier report, Then returns 400 Bad Request.")
    void testGenerateDossierReportBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/dossier/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }




    @WithMockUser(username = "epidemiologist", roles = "USER")
    @Test
    @DisplayName("Given an Epidemiologist user and a completed dossier report, When they submit a signature request, Then the dossier report is marked as signed and the signature is persisted.")
    void testSignDossierReportSuccess() throws Exception {
        User epiUser = new User();
        epiUser.setUsername("epidemiologist");
        epiUser.setRole("EPIDEMIOLOGIST");
        epiUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("epidemiologist")).thenReturn(Optional.of(epiUser));

        DossierReport report = new DossierReport("EMP-777", "FULL", "COMPLETED", "Test summary", 1, "/api/v1/dossier/reports/1/download");
        report = dossierReportRepository.saveAndFlush(report); // Try saveAndFlush instead of save

        Map<String, Object> request = Map.of("signature", "Dr. Epidemiologist Signature");

        mockMvc.perform(post("/api/v1/dossier/reports/{id}/sign", report.getId() != null ? report.getId() : 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_signed").value(true))
                .andExpect(jsonPath("$.signature").value("Dr. Epidemiologist Signature"));
    }

    @WithMockUser(username = "epidemiologist", roles = "USER")
    @Test
    @DisplayName("Given an invalid signature request, When they submit it, Then the system rejects it and returns a 400 Bad Request.")
    void testSignDossierReportInvalidRequest() throws Exception {
        User epiUser = new User();
        epiUser.setUsername("epidemiologist");
        epiUser.setRole("EPIDEMIOLOGIST");
        epiUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("epidemiologist")).thenReturn(Optional.of(epiUser));

        DossierReport report = new DossierReport("EMP-777", "FULL", "COMPLETED", "Test summary", 1, "/api/v1/dossier/reports/1/download");
        report = dossierReportRepository.saveAndFlush(report);

        Map<String, Object> request = Map.of("signature", ""); // Invalid empty signature

        mockMvc.perform(post("/api/v1/dossier/reports/{id}/sign", report.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @WithMockUser(username = "other_user", roles = "USER")
    @Test
    @DisplayName("Given a user outside Epidemiology department, When they submit a signature request, Then returns 403 Forbidden.")
    void testSignDossierReportForbiddenUser() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("other_user");
        otherUser.setRole("USER");
        otherUser.setDepartment("Вирусология");
        when(userRepository.findByUsername("other_user")).thenReturn(Optional.of(otherUser));

        DossierReport report = new DossierReport("EMP-777", "FULL", "COMPLETED", "Test summary", 1, "/api/v1/dossier/reports/1/download");
        report = dossierReportRepository.saveAndFlush(report);

        Map<String, Object> request = Map.of("signature", "Dr. Virologist");

        mockMvc.perform(post("/api/v1/dossier/reports/{id}/sign", report.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("FORBIDDEN"));
    }

    @WithMockUser(username = "epidemiologist", roles = "USER")
    @Test
    @DisplayName("Given a non-existent report ID, When signing, Then returns 404 Not Found.")
    void testSignDossierReportNotFound() throws Exception {
        User epiUser = new User();
        epiUser.setUsername("epidemiologist");
        epiUser.setRole("EPIDEMIOLOGIST");
        epiUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("epidemiologist")).thenReturn(Optional.of(epiUser));

        Map<String, Object> request = Map.of("signature", "Dr. Epidemiologist Signature");

        mockMvc.perform(post("/api/v1/dossier/reports/999999/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("NOT_FOUND"));
    }

    @WithMockUser(username = "epidemiologist", roles = "USER")
    @Test
    @DisplayName("Given an uncompleted report, When signing, Then returns 409 Conflict.")
    void testSignDossierReportConflictUncompleted() throws Exception {
        User epiUser = new User();
        epiUser.setUsername("epidemiologist");
        epiUser.setRole("EPIDEMIOLOGIST");
        epiUser.setDepartment("Эпидемиология");
        when(userRepository.findByUsername("epidemiologist")).thenReturn(Optional.of(epiUser));

        DossierReport report = new DossierReport("EMP-777", "FULL", "PENDING", "Test summary", 1, null);
        report = dossierReportRepository.saveAndFlush(report);

        Map<String, Object> request = Map.of("signature", "Dr. Epidemiologist Signature");

        mockMvc.perform(post("/api/v1/dossier/reports/{id}/sign", report.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("CONFLICT"));
    }

}
