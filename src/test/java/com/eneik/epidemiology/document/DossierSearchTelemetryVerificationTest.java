package com.eneik.epidemiology.document;

import com.eneik.epidemiology.telemetry.TelemetryEvent;
import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import com.eneik.epidemiology.telemetry.TelemetryService;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierSearchTelemetryVerificationTest {

    @Mock
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Mock
    private DossierReportRepository dossierReportRepository;

    @Mock
    private TelemetryEventRepository telemetryEventRepository;

    @Mock
    private UserRepository userRepository;

    private TelemetryService telemetryService;
    private EmployeeDossierController dossierController;
    private Clock fixedClock;

    private static final Instant TEST_INSTANT = Instant.parse("2026-09-05T00:00:00Z");

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
                new UsernamePasswordAuthenticationToken("test_user", "pass", Collections.emptyList())
        );

        User testUser = new User();
        testUser.setUsername("test_user");
        testUser.setRole("ADMIN");
        lenient().when(userRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Given search query with page and size, When searchEmployeeDocuments is called, Then paginated results and headers X-Total-Count and X-Total-Pages are returned")
    void testSearchEmployeeDocumentsPaginated() {
        EmployeeDocument doc1 = new EmployeeDocument();
        doc1.setId(101L);
        doc1.setEmployeeId("EMP-100");
        doc1.setEmployeeSurname("Иванов");
        doc1.setTitle("Приказ №1");

        EmployeeDocument doc2 = new EmployeeDocument();
        doc2.setId(102L);
        doc2.setEmployeeId("EMP-100");
        doc2.setEmployeeSurname("Иванов");
        doc2.setTitle("Отчет №2");

        Page<EmployeeDocument> mockPage = new PageImpl<>(
                List.of(doc1, doc2),
                PageRequest.of(0, 2),
                15 // Total 15 documents -> 8 pages with size 2
        );

        when(employeeDocumentRepository.searchEmployeeDocumentsSecure(
                eq(null), eq("Иванов"), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(true), eq(null), eq(null),
                any(Pageable.class)
        )).thenReturn(mockPage);

        ResponseEntity<?> response = dossierController.searchEmployeeDocuments(
                null, "Иванов", null, null, null, null, null, 0, 2
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders());
        assertEquals("15", response.getHeaders().getFirst("X-Total-Count"));
        assertEquals("8", response.getHeaders().getFirst("X-Total-Pages"));

        @SuppressWarnings("unchecked")
        List<EmployeeDocument> body = (List<EmployeeDocument>) response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals("Приказ №1", body.get(0).getTitle());
    }

    @Test
    @DisplayName("Given analysis speed measurement, When telemetry is emitted, Then TelemetryEvent has valid non-corrupted fields")
    void testTelemetryEmittedCorrectly() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime start = OffsetDateTime.of(2026, 9, 5, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 9, 5, 10, 5, 0, 0, ZoneOffset.UTC);

        TelemetryEvent event = telemetryService.recordAnalysisSpeedTelemetry("SESSION-QA-100", start, end, 300000L);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_ANALYSIS_SPEED_MEASURED, event.getEventType());
        assertEquals("SESSION-QA-100", event.getQueryTerm());
        assertEquals(300000L, event.getWorkflowDurationMs());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), event.getCreatedAt());

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryEventRepository, times(1)).save(captor.capture());
        TelemetryEvent savedEvent = captor.getValue();
        assertEquals("SESSION-QA-100", savedEvent.getQueryTerm());
        assertEquals(300000L, savedEvent.getWorkflowDurationMs());
    }

    @Test
    @DisplayName("Given zero search results, When recordSearchTelemetry is called, Then ZERO_RESULTS event is emitted")
    void testZeroResultsTelemetryEmitted() {
        when(telemetryEventRepository.save(any(TelemetryEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryEvent event = telemetryService.recordSearchTelemetry("Сидоров", 0);

        assertNotNull(event);
        assertEquals(TelemetryService.EVENT_ZERO_RESULTS, event.getEventType());
        assertEquals("Сидоров", event.getQueryTerm());
        assertEquals(0, event.getResultsCount());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), event.getCreatedAt());
    }
}
