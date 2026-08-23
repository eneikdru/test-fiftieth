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
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
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
}
