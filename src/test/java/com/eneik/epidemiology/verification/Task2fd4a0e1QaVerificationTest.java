package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.auth.AuthController;
import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@Transactional
class Task2fd4a0e1QaVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuthController authController;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(authController.getRestTemplate());

        // Seed sample document for catalog search test
        Document doc = new Document();
        doc.setTitle("Протокол расследования сальмонеллеза 2024");
        doc.setAuthorOrganization("Филиал НИИ Эпидемиологии");
        doc.setPublicationYear(2024);
        doc.setTextContent("Оперативный отчет и эпидемиологический протокол.");
        doc.setFilePath("/downloads/salmonella_outbreak.pdf");
        doc.setDocType("PROTOCOL");
        doc.setCreatedAt(OffsetDateTime.now());
        documentRepository.save(doc);
    }

    @Test
    @DisplayName("Given Moodle OAuth2 SSO login, When authenticating with valid token, Then authenticates seamlessly without 401 regressions")
    void testMoodleSsoAuthenticationSeamlessNo401Regression() throws Exception {
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer mock_valid_qa_token"))
                .andRespond(withSuccess(
                        "{\"username\":\"qa_moodle_researcher\",\"moodle_role\":\"Исследователь\",\"department\":\"BIO\",\"email\":\"qa_researcher@inst.ru\",\"full_name\":\"QA Researcher\",\"courses\":\"BIO101\"}",
                        MediaType.APPLICATION_JSON));

        String testFallbackPassword = System.getenv().getOrDefault("TEST_USER_PASSWORD", "test-password-placeholder");
        String ssoBody = String.format("{\"username\":\"qa_moodle_researcher\",\"moodle_token\":\"mock_valid_qa_token\",\"fallback_password\":\"%s\"}", testFallbackPassword);

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ssoBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("qa_moodle_researcher")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));

        User user = userRepository.findByUsername("qa_moodle_researcher").orElseThrow();
        assert "RESEARCHER".equals(user.getRole());
    }

    @Test
    @DisplayName("Given catalog search request, When query matches epidemiological protocol, Then returns matching catalog documents")
    @WithMockUser(username = "qa_user")
    void testCatalogSearchReturnsActualData() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "сальмонеллеза"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }
}
