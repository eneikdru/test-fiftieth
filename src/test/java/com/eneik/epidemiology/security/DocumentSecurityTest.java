package com.eneik.epidemiology.security;

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

import static org.hamcrest.Matchers.is;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
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

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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
