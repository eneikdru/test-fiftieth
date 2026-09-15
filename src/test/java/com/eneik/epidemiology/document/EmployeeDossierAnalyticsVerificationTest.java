package com.eneik.epidemiology.document;

import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeDossierAnalyticsVerificationTest {

    private MockMvc mockMvc;
    private EmployeeDocumentRepository employeeDocumentRepository;
    private DossierReportRepository dossierReportRepository;
    private TelemetryService telemetryService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        employeeDocumentRepository = Mockito.mock(EmployeeDocumentRepository.class);
        dossierReportRepository = Mockito.mock(DossierReportRepository.class);
        telemetryService = Mockito.mock(TelemetryService.class);
        userRepository = Mockito.mock(UserRepository.class);

        EmployeeDossierAnalyticsController controller = new EmployeeDossierAnalyticsController(
                employeeDocumentRepository,
                dossierReportRepository,
                telemetryService,
                userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Given non-empty documents dataset, when asserting metrics response, then no synthetic margin fields exist")
    void givenNonEmptyDataset_whenGetMetrics_thenNoSyntheticMarginFieldsExist() throws Exception {
        EmployeeDocument doc1 = new EmployeeDocument("EMP-777", "Sidorov", "REPORT", "Report 1", LocalDate.of(2023, 1, 1), "Details 1", "VIROLOGY");
        EmployeeDocument doc2 = new EmployeeDocument("EMP-777", "Sidorov", "ORDER", "Order 1", LocalDate.of(2023, 2, 1), "Details 2", "VIROLOGY");

        when(employeeDocumentRepository.searchEmployeeDocuments(
                eq("EMP-777"), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(doc1, doc2)));

        mockMvc.perform(get("/api/v1/dossier/analytics/metrics")
                        .param("employee_id", "EMP-777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_id").value("EMP-777"))
                .andExpect(jsonPath("$.metric_name").value("Доля научных отчетов в общем объеме документов"))
                .andExpect(jsonPath("$.denominator").value(2))
                .andExpect(jsonPath("$.value").value(closeTo(0.5, 0.0001)))
                .andExpect(jsonPath("$.marginOfError").doesNotExist())
                .andExpect(jsonPath("$.lower_bound").doesNotExist())
                .andExpect(jsonPath("$.upper_bound").doesNotExist());
    }

    @Test
    @DisplayName("Given sparse or empty test data, when requesting analytics, then response correctly reflects null value")
    void givenEmptyDataset_whenGetMetrics_thenValueIsNullAndNoSyntheticApproximation() throws Exception {
        when(employeeDocumentRepository.searchEmployeeDocuments(
                eq("EMP-999"), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/dossier/analytics/metrics")
                        .param("employee_id", "EMP-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_id").value("EMP-999"))
                .andExpect(jsonPath("$.metric_name").value("Доля научных отчетов в общем объеме документов"))
                .andExpect(jsonPath("$.denominator").value(0))
                .andExpect(jsonPath("$.value", nullValue()))
                .andExpect(jsonPath("$.marginOfError").doesNotExist())
                .andExpect(jsonPath("$.lower_bound").doesNotExist())
                .andExpect(jsonPath("$.upper_bound").doesNotExist());
    }
}
