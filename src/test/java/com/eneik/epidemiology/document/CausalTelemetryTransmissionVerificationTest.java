package com.eneik.epidemiology.document;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.telemetry.TelemetryEvent;
import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CausalTelemetryTransmissionVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    private String researcherToken;
    private User researcherUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        researcherUser = userService.createUser("causal_test_user", "TestPass123!", "RESEARCHER");
        researcherToken = jwtTokenProvider.generateToken(researcherUser.getUsername(), researcherUser.getRole());
    }

    @Test
    @DisplayName("Given a search request from User X, When executed, Then the integration test must assert the resulting telemetry record explicitly contains User X's ID")
    void testCausalTelemetry_ContainsUserId() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "causal_identity_mark_query")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken))
                .andExpect(status().isOk());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType("ZERO_RESULTS");
        assertFalse(events.isEmpty(), "Telemetry event should be stored for search");

        TelemetryEvent lastEvent = null;
        for (TelemetryEvent e : events) {
            if ("causal_identity_mark_query".equals(e.getQueryTerm())) {
                lastEvent = e;
            }
        }
        assertFalse(lastEvent == null, "Expected event not found");

        assertEquals(researcherUser.getId(), lastEvent.getUserId(), "Telemetry record explicitly contains User X's ID");
    }

    @Test
    @DisplayName("Given a request with a specific Trace ID, When executed, Then the telemetry record must contain that identical Trace ID")
    void testCausalTelemetry_ContainsTraceId() throws Exception {
        String testTraceId = "TRACE-CAUSAL-999";

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "causal_trace_mark_query")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken)
                        .header("X-Trace-Id", testTraceId))
                .andExpect(status().isOk());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType("ZERO_RESULTS");
        assertFalse(events.isEmpty(), "Telemetry event should be stored for search");

        TelemetryEvent lastEvent = null;
        for (TelemetryEvent e : events) {
            if ("causal_trace_mark_query".equals(e.getQueryTerm())) {
                lastEvent = e;
            }
        }
        assertFalse(lastEvent == null, "Expected event not found");

        assertEquals(testTraceId, lastEvent.getTraceId(), "Telemetry record explicitly contains identical Trace ID");
    }
}
