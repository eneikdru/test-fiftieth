package com.eneik.epidemiology.catalog;

import com.eneik.epidemiology.document.Document;
import com.eneik.epidemiology.document.DocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import com.eneik.epidemiology.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogControllerTest {

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

    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User admin = userService.createUser("admin_catalog_test", "AdminPass123!", "ADMIN");
        adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());
    }

    @Test
    @DisplayName("Given the Catalog API, When a search is requested, Then it returns a valid CatalogSearchResponse schema")
    void testSearchCatalogDocuments_ReturnsValidSchema() throws Exception {
        Document doc = new Document("Catalog Title", "Catalog Org", 2026, "/data/docs/uploads/cat.pdf");
        doc.setDocType("REPORT");
        documentRepository.save(doc);

        mockMvc.perform(get("/api/v1/catalog/documents")
                        .param("q", "Catalog Title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total_elements").isNumber())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].title", is("Catalog Title")))
                .andExpect(jsonPath("$.items[0].author_organization", is("Catalog Org")))
                .andExpect(jsonPath("$.items[0].publication_year", is(2026)))
                .andExpect(jsonPath("$.items[0].doc_type", is("REPORT")))
                .andExpect(jsonPath("$.items[0].created_at").exists());
    }
}
