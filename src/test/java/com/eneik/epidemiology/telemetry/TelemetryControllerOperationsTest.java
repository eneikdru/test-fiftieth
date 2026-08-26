package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.EpidemiologyApplication;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EpidemiologyApplication.class)
@AutoConfigureMockMvc
@Transactional
class TelemetryControllerOperationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User("admin_test", "hashed_pass", "ADMIN");
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);

        authToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
    }

    @Test
    @DisplayName("Given valid error rate metric, When recorded, Then ERROR_RATE event is stored")
    void testRecordErrorRateTelemetry() throws Exception {
        Map<String, String> request = Map.of("event_type", "ERROR_RATE");

        mockMvc.perform(post("/api/v1/telemetry/operations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_ERROR_RATE);
        assertEquals(1, events.size());

        TelemetryEvent event = events.get(0);
        assertEquals(TelemetryService.EVENT_ERROR_RATE, event.getEventType());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("Given valid pr reconciliation success metric, When recorded, Then PR_RECONCILIATION_SUCCESS event is stored")
    void testRecordPrReconciliationSuccessTelemetry() throws Exception {
        Map<String, String> request = Map.of("event_type", "PR_RECONCILIATION_SUCCESS");

        mockMvc.perform(post("/api/v1/telemetry/operations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_PR_RECONCILIATION_SUCCESS);
        assertEquals(1, events.size());

        TelemetryEvent event = events.get(0);
        assertEquals(TelemetryService.EVENT_PR_RECONCILIATION_SUCCESS, event.getEventType());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("Given invalid metric, When recorded, Then Bad Request is returned")
    void testRecordInvalidTelemetry() throws Exception {
        Map<String, String> request = Map.of("event_type", "INVALID_METRIC");

        mockMvc.perform(post("/api/v1/telemetry/operations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
