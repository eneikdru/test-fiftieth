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
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@AutoConfigureMockMvc
@Transactional
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String researcherToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User researcher = userService.createUser("researcher_doc_test", "ResPass123!", "RESEARCHER");
        researcherToken = jwtTokenProvider.generateToken(researcher.getUsername(), researcher.getRole());

        User admin = userService.createUser("admin_doc_test", "AdminPass123!", "ADMIN");
        adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());
    }

    @Test
    @DisplayName("Given a search query for an author, When the backend processes it, Then it returns the matching documents within 200ms")
    void testSearchByAuthor_ReturnsMatchingDocumentsWithin200ms() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("author", "НИИ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.results[0].authorOrganization", containsString("НИИ")));

        long durationMs = System.currentTimeMillis() - startTime;
        assertTrue(durationMs < 200, "Search query for author must execute within 200ms (took " + durationMs + "ms)");
    }

    @Test
    @DisplayName("Given an admin attempts to upload an unsupported file, When the backend validates it, Then it returns a clear error without crashing")
    void testUploadUnsupportedFile_ReturnsBadRequestWithClearRussianError() throws Exception {
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/x-msdownload",
                "binary content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(unsupportedFile)
                        .param("title", "Исполняемый файл")
                        .param("authorOrganization", "НИИ Эпидемиологии")
                        .param("publicationYear", "2024")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("UNSUPPORTED_FILE_TYPE")))
                .andExpect(jsonPath("$.message", containsString("Неподдерживаемый формат файла")));
    }

    @Test
    @DisplayName("Given an admin user, When uploading a supported document file, Then document is created successfully")
    void testAdminUploadSupportedFile_CreatesDocument() throws Exception {
        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "protocol_2024.pdf",
                "application/pdf",
                "PDF sample document content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(validFile)
                        .param("title", "Протокол 2024 года")
                        .param("authorOrganization", "Центр Эпидемиологии")
                        .param("publicationYear", "2024")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("успешно загружен")))
                .andExpect(jsonPath("$.document.title", is("Протокол 2024 года")))
                .andExpect(jsonPath("$.document.authorOrganization", is("Центр Эпидемиологии")))
                .andExpect(jsonPath("$.document.publicationYear", is(2024)));
    }

    @Test
    @DisplayName("Given a standard employee attempts to upload a document, When request reaches backend, Then it is blocked with 403 Forbidden")
    void testEmployeeUploadFile_ForbiddenWith403() throws Exception {
        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "protocol.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(validFile)
                        .param("title", "Протокол")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Given search parameters query and year, When search executes, Then filters matching documents")
    void testSearchByTitleAndYear_ReturnsMatchingDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "сальмонеллеза")
                        .param("year", "2023")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + researcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.results[0].publicationYear", is(2023)));
    }
}
