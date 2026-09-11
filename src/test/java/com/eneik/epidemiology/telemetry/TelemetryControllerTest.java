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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    @DisplayName("Given valid telemetry data, When sent to ingestion endpoint, Then it is processed, saved, and returned")
    void testIngestTelemetryEventSuccess() throws Exception {
        String jsonPayload = """
                {
                    "eventType": "NAVIGATE_TAB",
                    "module": "Core Module",
                    "title": "Dashboard Loaded",
                    "workflowDurationMs": 120,
                    "success": true
                }
                """;

        mockMvc.perform(post("/api/v1/telemetry/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.eventType", is("NAVIGATE_TAB")))
                .andExpect(jsonPath("$.module", is("Core Module")))
                .andExpect(jsonPath("$.title", is("Dashboard Loaded")))
                .andExpect(jsonPath("$.workflowDurationMs", is(120)))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType("NAVIGATE_TAB");
        assertEquals(1, events.size());
        assertEquals("Core Module", events.get(0).getModule());
    }

    @Test
    @DisplayName("Given invalid telemetry request without eventType, When sent to ingestion endpoint, Then 400 Bad Request is returned")
    void testIngestTelemetryEventValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "module": "Core Module",
                    "title": "Invalid Event"
                }
                """;

        mockMvc.perform(post("/api/v1/telemetry/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Given telemetry events in database, When querying GET endpoint with and without filters, Then actual telemetry data is returned")
    void testGetTelemetryEvents() throws Exception {
        TelemetryEvent event1 = new TelemetryEvent("NAVIGATE_TAB", "dashboard", null, null, OffsetDateTime.now());
        event1.setModule("Core Module");
        telemetryEventRepository.save(event1);

        TelemetryEvent event2 = new TelemetryEvent("DOWNLOAD_SUCCESS", null, 101L, null, OffsetDateTime.now());
        telemetryEventRepository.save(event2);

        // All events
        mockMvc.perform(get("/api/v1/telemetry/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Filter by eventType
        mockMvc.perform(get("/api/v1/telemetry/events")
                        .param("eventType", "NAVIGATE_TAB")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType", is("NAVIGATE_TAB")));

        // Filter by documentId
        mockMvc.perform(get("/api/v1/telemetry/events")
                        .param("documentId", "101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentId", is(101)));
    }

    @Test
    @DisplayName("Given unauthenticated request to telemetry endpoint, Then 401 Unauthorized is returned")
    void testUnauthorizedTelemetryAccess() throws Exception {
        mockMvc.perform(get("/api/v1/telemetry/events"))
                .andExpect(status().isUnauthorized());
    }
}
