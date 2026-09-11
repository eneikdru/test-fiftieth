package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User("researcher_test", "hashed_pass", "RESEARCHER");
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);

        authToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
    }

    @Test
    @DisplayName("Given search yields zero results, When search API concludes, Then zero-results event is logged in database")
    void testZeroResultsSearchTelemetryIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "несуществующий_протокол")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_ZERO_RESULTS);
        assertEquals(1, events.size());

        TelemetryEvent event = events.get(0);
        assertEquals("несуществующий_протокол", event.getQueryTerm());
        assertEquals(0, event.getResultsCount());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("Given user downloads a document, When action completes, Then download success event is recorded in database")
    void testDownloadSuccessTelemetryIntegration() throws Exception {
        Long docId = 42L;

        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOWNLOAD_SUCCESS);
        assertEquals(1, events.size());

        TelemetryEvent event = events.get(0);
        assertEquals(docId, event.getDocumentId());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("Given valid telemetry payload, When sent to POST /api/v1/telemetry/events, Then event is created and saved")
    void testIngestTelemetryEventSuccess() throws Exception {
        String jsonPayload = """
            {
                "eventType": "SPA_NAVIGATION",
                "module": "Core Module",
                "title": "Dashboard Loaded",
                "success": true,
                "processingTimeMs": 24
            }
            """;

        mockMvc.perform(post("/api/v1/telemetry/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.eventType", is("SPA_NAVIGATION")))
                .andExpect(jsonPath("$.module", is("Core Module")))
                .andExpect(jsonPath("$.title", is("Dashboard Loaded")))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.processingTimeMs", is(24)));

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType("SPA_NAVIGATION");
        assertEquals(1, events.size());
        assertEquals("Core Module", events.get(0).getModule());
    }

    @Test
    @DisplayName("Given payload with missing eventType, When sent to ingestion endpoint, Then 400 Bad Request is returned")
    void testIngestTelemetryEventMissingTypeReturnsBadRequest() throws Exception {
        String invalidJsonPayload = """
            {
                "module": "Core Module",
                "title": "Dashboard Loaded"
            }
            """;

        mockMvc.perform(post("/api/v1/telemetry/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_EVENT_TYPE")))
                .andExpect(jsonPath("$.message", is("Тип события обязателен.")));
    }

    @Test
    @DisplayName("Given existing telemetry events, When GET /api/v1/telemetry/events is queried, Then all events are returned")
    void testGetTelemetryEventsAll() throws Exception {
        TelemetryEvent event1 = new TelemetryEvent("PAGE_VIEW", null, null, null, OffsetDateTime.now());
        event1.setModule("Analytics");
        telemetryEventRepository.save(event1);

        TelemetryEvent event2 = new TelemetryEvent("EXPORT_PDF", null, 101L, 1, OffsetDateTime.now());
        event2.setModule("Export");
        telemetryEventRepository.save(event2);

        mockMvc.perform(get("/api/v1/telemetry/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Given existing events, When queried by eventType filter, Then matching events are returned")
    void testGetTelemetryEventsFilteredByEventType() throws Exception {
        TelemetryEvent event1 = new TelemetryEvent("FILTER_MATCH", "query1", null, null, OffsetDateTime.now());
        telemetryEventRepository.save(event1);

        TelemetryEvent event2 = new TelemetryEvent("OTHER_TYPE", "query2", null, null, OffsetDateTime.now());
        telemetryEventRepository.save(event2);

        mockMvc.perform(get("/api/v1/telemetry/events")
                        .param("eventType", "FILTER_MATCH")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType", is("FILTER_MATCH")));
    }

    @Test
    @DisplayName("Given existing events, When queried by module filter, Then matching events are returned")
    void testGetTelemetryEventsFilteredByModule() throws Exception {
        TelemetryEvent event1 = new TelemetryEvent("NAVIGATE", null, null, null, OffsetDateTime.now());
        event1.setModule("Data Module");
        telemetryEventRepository.save(event1);

        TelemetryEvent event2 = new TelemetryEvent("NAVIGATE", null, null, null, OffsetDateTime.now());
        event2.setModule("Export Module");
        telemetryEventRepository.save(event2);

        mockMvc.perform(get("/api/v1/telemetry/events")
                        .param("module", "Data Module")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].module", is("Data Module")));
    }

    @Test
    @DisplayName("Given existing event ID, When GET by ID endpoint is called, Then single event or 404 is returned")
    void testGetTelemetryEventById() throws Exception {
        TelemetryEvent event = new TelemetryEvent("SINGLE_EVENT", null, null, null, OffsetDateTime.now());
        event.setTitle("Test Event");
        TelemetryEvent saved = telemetryEventRepository.save(event);

        mockMvc.perform(get("/api/v1/telemetry/events/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.eventType", is("SINGLE_EVENT")))
                .andExpect(jsonPath("$.title", is("Test Event")));

        mockMvc.perform(get("/api/v1/telemetry/events/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
