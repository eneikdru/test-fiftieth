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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase(type = io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;
    @Autowired
    private DocumentRepository documentRepository;

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
        Document doc = new Document();
        doc.setTitle("Test Doc");
        doc.setFilePath("data/docs/uploads/test_doc.pdf");
        doc.setAuthorOrganization("Test Org");
        doc.setPublicationYear(2023);
        doc = documentRepository.save(doc);
        Long docId = doc.getId();

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
