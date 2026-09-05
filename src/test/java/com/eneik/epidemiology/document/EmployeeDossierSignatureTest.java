package com.eneik.epidemiology.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeDossierSignatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        dossierReportRepository.deleteAll();

        User epidemiologist = new User();
        epidemiologist.setUsername("epiUser");
        epidemiologist.setRole("EPIDEMIOLOGIST");
        when(userRepository.findByUsername("epiUser")).thenReturn(Optional.of(epidemiologist));

        User standardUser = new User();
        standardUser.setUsername("stdUser");
        standardUser.setRole("USER");
        when(userRepository.findByUsername("stdUser")).thenReturn(Optional.of(standardUser));
    }

    @Test
    @Transactional
    @WithMockUser(username = "epiUser", roles = "EPIDEMIOLOGIST")
    @DisplayName("Given I am logged in as an Epidemiologist, When I submit a signature for a generated dossier report, Then the system records the signature/approval successfully.")
    void testSignReportPositive() throws Exception {
        DossierReport report = new DossierReport("EMP-123", "SUMMARY", "COMPLETED", "Summary", 1, null);
        report = dossierReportRepository.save(report);

        mockMvc.perform(post("/api/v1/dossier/reports/" + report.getId() + "/sign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));

        mockMvc.perform(get("/api/v1/dossier/reports/" + report.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.signed_by").value("epiUser"))
                .andExpect(jsonPath("$.signed_at").exists());
    }

    @Test
    @Transactional
    @WithMockUser(username = "epiUser", roles = "EPIDEMIOLOGIST")
    @DisplayName("Given a pending report, When an epidemiologist signs, Then returns conflict.")
    void testSignReportNegativePending() throws Exception {
        DossierReport report = new DossierReport("EMP-123", "SUMMARY", "PENDING", "Summary", 1, null);
        report = dossierReportRepository.save(report);

        mockMvc.perform(post("/api/v1/dossier/reports/" + report.getId() + "/sign"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("CONFLICT"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "stdUser", roles = "USER")
    @DisplayName("Given I am a standard user, When I submit a signature, Then access is forbidden.")
    void testSignReportNegativeForbidden() throws Exception {
        DossierReport report = new DossierReport("EMP-123", "SUMMARY", "COMPLETED", "Summary", 1, null);
        report = dossierReportRepository.save(report);

        mockMvc.perform(post("/api/v1/dossier/reports/" + report.getId() + "/sign"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("FORBIDDEN"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "epiUser", roles = "EPIDEMIOLOGIST")
    @DisplayName("Given a report, When atomic sign method is called concurrently, Then only one succeeds.")
    void testSignReportAtomicBoundary() {
        DossierReport report = new DossierReport("EMP-123", "SUMMARY", "COMPLETED", "Summary", 1, null);
        report = dossierReportRepository.save(report);
        Long id = report.getId();

        int successCount = dossierReportRepository.signReport(id, "COMPLETED", "SIGNED", "epiUser", OffsetDateTime.now());
        org.junit.jupiter.api.Assertions.assertEquals(1, successCount);

        int failureCount = dossierReportRepository.signReport(id, "COMPLETED", "SIGNED", "otherEpiUser", OffsetDateTime.now());
        org.junit.jupiter.api.Assertions.assertEquals(0, failureCount);
    }
}
