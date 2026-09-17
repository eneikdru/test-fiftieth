package com.eneik.epidemiology.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProtocolControllerTest {

    private DocumentRepository documentRepository;
    private MockMvc mockMvc;

    static class TestDocumentSearchResult implements DocumentSearchResultProjection {
        private final Long id;
        private final String title;
        private final String docType;

        public TestDocumentSearchResult(Long id, String title, String docType) {
            this.id = id;
            this.title = title;
            this.docType = docType;
        }

        @Override public Long getId() { return id; }
        @Override public String getTitle() { return title; }
        @Override public String getDocType() { return docType; }
        @Override public String getAuthorOrganization() { return "НИИ Эпидемиологии"; }
        @Override public Integer getPublicationYear() { return 2024; }
        @Override public LocalDate getPublicationDate() { return LocalDate.of(2024, 1, 1); }
        @Override public String getFilePath() { return "/docs/p1.pdf"; }
        @Override public String getTextContent() { return null; }
        @Override public OffsetDateTime getCreatedAt() { return OffsetDateTime.now(); }
        @Override public Double getRelevanceScore() { return 1.0; }
    }

    @BeforeEach
    void setUp() {
        documentRepository = Mockito.mock(DocumentRepository.class);
        ProtocolController protocolController = new ProtocolController(documentRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(protocolController).build();
    }

    @Test
    @DisplayName("Given protocol search request, When controller handles request, Then returns list of protocol documents")
    void testGetProtocols() throws Exception {
        TestDocumentSearchResult testResult = new TestDocumentSearchResult(101L, "Epidemiological Protocol Salmonella", "PROTOCOL");

        Mockito.when(documentRepository.fullTextSearch(eq("Salmonella"), eq("PROTOCOL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testResult)));

        mockMvc.perform(get("/api/v1/protocols?q=Salmonella"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocols", hasSize(1)))
                .andExpect(jsonPath("$.protocols[0].id", is(101)))
                .andExpect(jsonPath("$.protocols[0].title", is("Epidemiological Protocol Salmonella")));
    }

    @Test
    @DisplayName("Given protocol ID search, When protocol exists, Then returns protocol details")
    void testGetProtocolById_Found() throws Exception {
        Document protocolDoc = new Document("Protocol COVID-19", "Epidemiology Inst", 2023, "/docs/p2.pdf");
        protocolDoc.setId(202L);
        protocolDoc.setDocType("PROTOCOL");

        Mockito.when(documentRepository.findById(202L)).thenReturn(Optional.of(protocolDoc));

        mockMvc.perform(get("/api/v1/protocols/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(202)))
                .andExpect(jsonPath("$.title", is("Protocol COVID-19")));
    }

    @Test
    @DisplayName("Given protocol ID search for a non-protocol document, When accessed, Then returns 404 Not Found")
    void testGetProtocolById_NonProtocolType_NotFound() throws Exception {
        Document generalDoc = new Document("General Report", "Epidemiology Inst", 2023, "/docs/r1.pdf");
        generalDoc.setId(303L);
        generalDoc.setDocType("REPORT");

        Mockito.when(documentRepository.findById(303L)).thenReturn(Optional.of(generalDoc));

        mockMvc.perform(get("/api/v1/protocols/303"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given protocol ID search, When protocol does not exist, Then returns 404 Not Found")
    void testGetProtocolById_NotFound() throws Exception {
        Mockito.when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/protocols/999"))
                .andExpect(status().isNotFound());
    }
}
