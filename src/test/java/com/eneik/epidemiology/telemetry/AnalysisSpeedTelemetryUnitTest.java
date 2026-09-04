package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.document.EmployeeDossierController;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisSpeedTelemetryUnitTest {

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    @Mock
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Mock
    private DossierReportRepository dossierReportRepository;

    @Mock
    private UserRepository userRepository;

    private TelemetryService telemetryService;
    private EmployeeDossierController dossierController;
    private Clock fixedClock;

    private static final Instant TEST_INSTANT = Instant.parse("2026-08-28T12:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
        telemetryService = new TelemetryService(telemetryEventRepository, fixedClock);
        dossierController = new EmployeeDossierController(
                employeeDocumentRepository,
                dossierReportRepository,
                telemetryService,
                userRepository
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("analyst_test", "pass", Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Given session timestamps, When recordAnalysisSpeedTelemetry is called, Then TelemetryEvent is saved with computed duration")
    void testRecordAnalysisSpeedTelemetryComputation() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime start = OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 8, 28, 10, 15, 0, 0, ZoneOffset.UTC); // 15 min = 900,000 ms

        TelemetryEvent event = telemetryService.recordAnalysisSpeedTelemetry("SESS-001", start, end, null);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, event.getEventType());
        assertEquals("SESS-001", event.getQueryTerm());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
        assertEquals(900000L, event.getWorkflowDurationMs());

        verify(telemetryEventRepository, times(1)).save(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("Given dossier request with session telemetry, When generateDossierReport executes, Then analysis speed telemetry is emitted")
    void testGenerateDossierReportEmitsAnalysisSpeedMetric() {
        when(employeeDocumentRepository.findUnifiedEmployeeDossier("EMP-001"))
                .thenReturn(Collections.emptyList());

        DossierReport savedReport = new DossierReport("EMP-001", "SUMMARY", "PENDING", null, 0, null);
        savedReport.setId(42L);
        savedReport.setCreatedAt(OffsetDateTime.of(2026, 8, 28, 12, 0, 0, 0, ZoneOffset.UTC));

        when(dossierReportRepository.save(any(DossierReport.class))).thenReturn(savedReport);
        when(dossierReportRepository.updateStatus(any(), eq("PENDING"), eq("COMPLETED"))).thenReturn(1);
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime start = OffsetDateTime.of(2026, 8, 28, 11, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 8, 28, 11, 20, 0, 0, ZoneOffset.UTC); // 20 min = 1,200,000 ms

        Map<String, Object> request = Map.of(
                "employee_id", "EMP-001",
                "template_type", "SUMMARY",
                "session_id", "SESS-002",
                "session_start_time", start.toString(),
                "session_end_time", end.toString()
        );

        ResponseEntity<?> response = dossierController.generateDossierReport(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(telemetryEventRepository, times(1)).save(argThat(event ->
                TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED.equals(event.getEventType()) &&
                "SESS-002".equals(event.getQueryTerm()) &&
                Long.valueOf(1200000L).equals(event.getWorkflowDurationMs())
        ));
    }
}
