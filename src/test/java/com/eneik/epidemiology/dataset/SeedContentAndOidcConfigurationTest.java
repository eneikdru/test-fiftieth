package com.eneik.epidemiology.dataset;

import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
class SeedContentAndOidcConfigurationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given a fresh application startup, When initialization completes, Then the integration test must assert the presence of the seeded epidemiological documents.")
    void testSeededEpidemiologicalDocumentsPresentOnStartup() {
        List<Document> documents = documentRepository.findAll();

        assertThat(documents)
                .as("Seeded epidemiological documents should be present in repository upon startup")
                .isNotEmpty();

        assertThat(documents)
                .extracting(Document::getTitle)
                .anyMatch(title -> title.contains("COVID-19"))
                .anyMatch(title -> title.contains("кори"))
                .anyMatch(title -> title.contains("гриппа"))
                .anyMatch(title -> title.contains("туберкулеза"));

        Document covidDoc = documents.stream()
                .filter(d -> d.getTitle().contains("COVID-19"))
                .findFirst()
                .orElseThrow();

        assertThat(covidDoc.getAuthorOrganization()).isEqualTo("НИИ Инфектологии");
        assertThat(covidDoc.getPublicationYear()).isEqualTo(2020);
        assertThat(covidDoc.getDocType()).isEqualTo("REPORT");
    }

    @Test
    @DisplayName("Given the simulated OIDC configuration, When verified, Then it must be present and correctly formatted.")
    void testSimulatedOidcConfigurationPresentAndFormatted() throws Exception {
        mockMvc.perform(get("/api/v1/auth/moodle/config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_url", startsWith("https://moodle.epidemiology-inst.ru/oauth2/authorize")))
                .andExpect(jsonPath("$.login_url", containsString("client_id=epidemiology_portal")))
                .andExpect(jsonPath("$.login_url", containsString("response_type=code")))
                .andExpect(jsonPath("$.login_url", containsString("redirect_uri=http://localhost:8080/auth/moodle/callback")))
                .andExpect(jsonPath("$.login_url", containsString("state=")))
                .andExpect(jsonPath("$.auth_url", is(notNullValue())));
    }
}
