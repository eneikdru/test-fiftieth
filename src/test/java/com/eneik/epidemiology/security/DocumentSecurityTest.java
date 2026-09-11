package com.eneik.epidemiology.security;

import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentSecurityTest {

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

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Given an authenticated user without RESEARCHER, EPIDEMIOLOGIST or ADMIN role (USER role), When attempting to access documents via /api/v1/documents, Then they are not able to retrieve PROTOCOL documents")
    void testUserRoleCannotAccessProtocolDocuments() throws Exception {
        Document protocolDoc = new Document("Секретный Протокол №1", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/p1.pdf");
        protocolDoc.setDocType("PROTOCOL");
        documentRepository.save(protocolDoc);

        Document generalDoc = new Document("Открытый Доклад", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/r1.pdf");
        generalDoc.setDocType("REPORT");
        documentRepository.save(generalDoc);

        User regularUser = userService.createUser("user_ordinary", "UserPass123!", "USER");
        String userToken = jwtTokenProvider.generateToken(regularUser.getUsername(), regularUser.getRole());

        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].docType", is("REPORT")))
                .andExpect(jsonPath("$[0].title", is("Открытый Доклад")));

        mockMvc.perform(get("/api/v1/documents/search")
                .param("q", "Протокол")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", is(1)))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("Given an authorized user with RESEARCHER role, When requesting documents, Then PROTOCOL documents are successfully returned")
    void testResearcherRoleCanAccessProtocolDocuments() throws Exception {
        Document protocolDoc = new Document("Секретный Протокол №2", "НИИ Эпидемиологии", 2024, "/data/docs/uploads/p2.pdf");
        protocolDoc.setDocType("PROTOCOL");
        documentRepository.save(protocolDoc);

        User researcher = userService.createUser("researcher_olga_proto", "ResPass123!", "RESEARCHER");
        String researcherToken = jwtTokenProvider.generateToken(researcher.getUsername(), researcher.getRole());

        mockMvc.perform(get("/api/v1/documents")
                .header("Authorization", "Bearer " + researcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].docType", is("PROTOCOL")));
    }

    @Test
    @DisplayName("Given an employee (RESEARCHER/USER) attempts to delete a document, When the request reaches the backend, Then the role middleware blocks it with a 403 status")
    void testEmployeeDeleteDocument_BlockedWith403Forbidden() throws Exception {
        User researcher = userService.createUser("researcher_olga", "ResPass123!", "RESEARCHER");
        String researcherToken = jwtTokenProvider.generateToken(researcher.getUsername(), researcher.getRole());

        mockMvc.perform(delete("/api/v1/documents/42")
                .header("Authorization", "Bearer " + researcherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", is("Недостаточно прав для выполнения действия. Удаление доступно только администратору.")));
    }

    @Test
    @DisplayName("Given an unauthenticated request to delete a document, When request arrives, Then returns 401 Unauthorized")
    void testUnauthenticatedDeleteDocument_Returns401Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Требуется авторизация для выполнения данной операции.")));
    }

    @Test
    @DisplayName("Given an admin user attempts to delete a document, When request is processed, Then returns 200 OK")
    void testAdminDeleteDocument_Allowed() throws Exception {
        User admin = userService.createUser("admin_boris", "AdminPass123!", "ADMIN");
        String adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());

        mockMvc.perform(delete("/api/v1/documents/42")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Документ 42 успешно удален.")));
    }
}
