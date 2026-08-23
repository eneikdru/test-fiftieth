package com.eneik.epidemiology.document;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin = new User("admin_boris", "AdminPass123!", "ADMIN");
        admin.setCreatedAt(OffsetDateTime.now());
        userRepository.save(admin);

        adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());
    }

    @Test
    @DisplayName("Given an admin attempts to upload an unsupported file, When the backend validates it, Then it returns a clear error without crashing.")
    void testUploadUnsupportedFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.sh",
                "application/x-sh",
                "echo 'hacked'".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                .file(file)
                .param("title", "Test Title")
                .param("author_organization", "Test Author")
                .param("publication_year", "2023")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", is("Неподдерживаемый формат файла.")));
    }

    @Test
    @DisplayName("Given an admin attempts to upload a supported file, When the backend validates it, Then it returns 201 Created and persists the document metadata.")
    void testUploadSupportedFile_Returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "pdf content".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                .file(file)
                .param("title", "Test PDF")
                .param("author_organization", "Test Author")
                .param("publication_year", "2023")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Test PDF")))
                .andExpect(jsonPath("$.author_organization", is("Test Author")))
                .andExpect(jsonPath("$.publication_year", is(2023)));
    }
}
