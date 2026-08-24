package com.eneik.epidemiology.document;

import com.eneik.epidemiology.security.JwtTokenProvider;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PdfSearchVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DocumentRepository documentRepository;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = userService.createUser("pdf_search_tester", "TestPass123!", "RESEARCHER");
        token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        // Create sample documents representing orders, extracts, reports, exam results
        Document doc1 = new Document("Приказ №102 о назначении", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/order102.pdf");
        doc1.setDocType("ORDER");
        doc1.setPublicationDate(LocalDate.of(2024, 3, 15));
        doc1.setTextContent("Приказ по институту. Включить исследователя Петрова В.И. в состав комиссии.");
        documentRepository.save(doc1);

        Document doc2 = new Document("Выписка из протокола учёного совета", "Учёный совет НИИ", 2024, "/data/docs/uploads/extract2024.pdf");
        doc2.setDocType("EXTRACT");
        doc2.setPublicationDate(LocalDate.of(2024, 5, 20));
        doc2.setTextContent("Выписка из заседания. Докладчик Сидоров А.А. представил отчет по эпидемиологии.");
        documentRepository.save(doc2);

        Document doc3 = new Document("Результаты весенних экзаменов", "Факультет Эпидемиологии", 2023, "/data/docs/uploads/exams2023.pdf");
        doc3.setDocType("EXAM_RESULT");
        doc3.setPublicationDate(LocalDate.of(2023, 6, 10));
        doc3.setTextContent("Результаты экзаменационной сессии. Студент Иванов С.П. сдал экзамен на отлично.");
        documentRepository.save(doc3);
    }

    @Test
    @DisplayName("Given full-text search by surname in document content, When requested, Then returns matching document with highlighted snippet")
    void testPdfSearchBySurname_ReturnsMatchesWithHighlights() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "Петрова")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", is(1)))
                .andExpect(jsonPath("$.items[0].title", containsString("Приказ №102")))
                .andExpect(jsonPath("$.items[0].doc_type", is("ORDER")))
                .andExpect(jsonPath("$.items[0].highlights[0]", containsString("<em>Петрова</em>")));
    }

    @Test
    @DisplayName("Given full-text search with doc_type filter, When requested, Then filters strictly by doc_type")
    void testPdfSearchByDocTypeFilter_ReturnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "эпидемиологии")
                        .param("doc_type", "EXAM_RESULT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", is(1)))
                .andExpect(jsonPath("$.items[0].doc_type", is("EXAM_RESULT")))
                .andExpect(jsonPath("$.items[0].title", containsString("Результаты весенних экзаменов")));
    }

    @Test
    @DisplayName("Given full-text search with date range filters, When requested, Then returns documents within date range")
    void testPdfSearchByDateRange_ReturnsDocumentsInRange() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "институту")
                        .param("from_date", "2024-01-01")
                        .param("to_date", "2024-12-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", is(1)))
                .andExpect(jsonPath("$.items[0].title", containsString("Приказ №102")));
    }

    @Test
    @DisplayName("Given legacy query search by author and year, When requested, Then returns formatted result list")
    void testLegacySearchByAuthorAndYear_ReturnsMatchingDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("author", "Учёный совет")
                        .param("year", "2024")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.results[0].authorOrganization", containsString("Учёный совет")));
    }
}
