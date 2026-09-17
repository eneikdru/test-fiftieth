package com.eneik.epidemiology.document;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CalibratedScoringQATest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private String researcherToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User researcherUser = userService.createUser("scoring_qa_user", "ResPass123!", "RESEARCHER");
        researcherToken = jwtTokenProvider.generateToken(researcherUser.getUsername(), researcherUser.getRole());
    }

    @Test
    @DisplayName("Given strong and weak match documents, When search executed, Then strong match score is strictly greater than weak match score")
    void testRelevanceScore_StrongMatchGreaterThanWeakMatch() throws Exception {
        Document strongMatchDoc = new Document("Калибровка", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/strong.pdf");
        strongMatchDoc.setDocType("REPORT");
        documentRepository.save(strongMatchDoc);

        Document weakMatchDoc = new Document("Общий отчет", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/weak.pdf");
        weakMatchDoc.setDocType("REPORT");
        weakMatchDoc.setTextContent("В тексте присутствует калибровка системного уровня.");
        documentRepository.save(weakMatchDoc);

        MvcResult result = mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "Калибровка")
                        .param("docType", "REPORT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode items = json.get("items");

        assertTrue(items.size() >= 2, "Expected at least 2 search result items");

        double score0 = items.get(0).get("relevance_score").asDouble();
        double score1 = items.get(1).get("relevance_score").asDouble();

        assertTrue(score0 > score1, "Strong match relevance score (" + score0 + ") must be strictly greater than weak match score (" + score1 + ")");
    }

    @Test
    @DisplayName("Given search query matching documents with different match quality, When executed, Then relevance scores are not uniformly 1.0")
    void testRelevanceScore_NotUniformlyOne() throws Exception {
        Document exactDoc = new Document("Эпидемиология", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/doc1.pdf");
        exactDoc.setDocType("REPORT");
        documentRepository.save(exactDoc);

        Document contentDoc = new Document("Справочник медицинский", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/doc2.pdf");
        contentDoc.setDocType("REPORT");
        contentDoc.setTextContent("Раздел посвящен предмету эпидемиология.");
        documentRepository.save(contentDoc);

        MvcResult result = mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "Эпидемиология")
                        .param("docType", "REPORT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode items = json.get("items");

        assertTrue(items.size() >= 1, "Expected search result items");

        boolean hasNonOneScore = false;
        for (JsonNode item : items) {
            double score = item.get("relevance_score").asDouble();
            if (Math.abs(score - 1.0) > 1e-6) {
                hasNonOneScore = true;
                break;
            }
        }

        assertTrue(hasNonOneScore, "Relevance scores across results must not be uniformly 1.0");
    }
}
