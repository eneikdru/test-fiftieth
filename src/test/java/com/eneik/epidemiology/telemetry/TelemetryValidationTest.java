package com.eneik.epidemiology.telemetry;

import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelemetryValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User("qa_tester", "hashed_pass", "RESEARCHER");
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);

        authToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
    }

    @Test
    @DisplayName("Given a test search returns no results, When the logs are inspected, Then the corresponding empty-search metric is present")
    void givenEmptySearch_whenSearchExecutes_thenEmptySearchMetricIsPresent() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "non_existent_query")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_ZERO_RESULTS);
        assertEquals(1, events.size(), "Expected exactly one ZERO_RESULTS telemetry metric event.");

        TelemetryEvent event = events.get(0);
        assertEquals(TelemetryService.EVENT_ZERO_RESULTS, event.getEventType());
        assertEquals("non_existent_query", event.getQueryTerm());
        assertEquals(0, event.getResultsCount());
        assertNotNull(event.getCreatedAt(), "Metric event timestamp must be present.");
    }

    @Test
    @DisplayName("Given a test download with a properly setup document, When the action completes, Then exactly one successful download metric is recorded")
    void givenTestDownload_whenActionCompletes_thenExactlyOneDownloadMetricIsRecorded() throws Exception {
        Document document = new Document("Тестовый документ", "НИИ Эпидемиологии", 2024, "/docs/test.pdf");
        Document savedDoc = documentRepository.save(document);
        Long docId = savedDoc.getId();

        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken))
                .andExpect(status().isOk());

        List<TelemetryEvent> events = telemetryEventRepository.findByEventType(TelemetryService.EVENT_DOWNLOAD_SUCCESS);
        assertEquals(1, events.size(), "Expected exactly one DOWNLOAD_SUCCESS telemetry metric event.");

        TelemetryEvent event = events.get(0);
        assertEquals(TelemetryService.EVENT_DOWNLOAD_SUCCESS, event.getEventType());
        assertEquals(docId, event.getDocumentId());
        assertNotNull(event.getCreatedAt(), "Metric event timestamp must be present.");
    }
}
