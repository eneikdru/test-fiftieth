package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.strain.Strain;
import com.eneik.epidemiology.strain.StrainRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
class StrainManagementAndMoodleSsoQaVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StrainRepository strainRepository;

    private UUID deptStrainId;

    @BeforeEach
    void setUp() {
        strainRepository.deleteAll();
        userRepository.deleteAll();

        // Seed Users
        User researcher = new User();
        researcher.setUsername("researcher_bio");
        researcher.setPasswordHash("hash");
        researcher.setRole("RESEARCHER");
        researcher.setDepartment("BIO");
        researcher.setCourses("BIO101");
        researcher.setCreatedAt(OffsetDateTime.now());
        userRepository.save(researcher);

        User admin = new User();
        admin.setUsername("admin_user");
        admin.setPasswordHash("hash");
        admin.setRole("ADMIN");
        admin.setCreatedAt(OffsetDateTime.now());
        userRepository.save(admin);

        // Seed Strains
        Strain deptStrain = new Strain();
        deptStrainId = UUID.randomUUID();
        deptStrain.setId(deptStrainId);
        deptStrain.setName("Bio-Pathogen-A");
        deptStrain.setAccessDepartment("BIO");
        strainRepository.save(deptStrain);
    }

    @Test
    @DisplayName("Given Strain Management API, When test CRUD operations are performed, Then they succeed end-to-end")
    @WithMockUser(username = "researcher_bio")
    void testStrainManagementCrudEndToEnd() throws Exception {
        // 1. CREATE Strain
        String createJson = """
                {
                    "name": "Influenza H5N1",
                    "description": "Avian Influenza Strain",
                    "originCountry": "Vietnam",
                    "severityLevel": "HIGH",
                    "accessDepartment": "BIO",
                    "accessCourse": "BIO101"
                }
                """;

        String responseStr = mockMvc.perform(post("/api/v1/strains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Influenza H5N1"))
                .andReturn().getResponse().getContentAsString();

        // Extract ID
        String strainId = responseStr.split("\"id\":\"")[1].split("\"")[0];

        // 2. READ Strains
        mockMvc.perform(get("/api/v1/strains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Influenza H5N1')]").exists());

        // 3. UPDATE Strain
        String updateJson = """
                {
                    "name": "Influenza H5N1 Mutated",
                    "description": "Updated strain details",
                    "accessDepartment": "BIO",
                    "accessCourse": "BIO101"
                }
                """;

        mockMvc.perform(put("/api/v1/strains/" + strainId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Influenza H5N1 Mutated"));

        // 4. DELETE Strain
        mockMvc.perform(delete("/api/v1/strains/" + strainId))
                .andExpect(status().isNoContent());

        assertTrue(strainRepository.findById(UUID.fromString(strainId)).isEmpty());
    }

    @Test
    @DisplayName("Given Moodle Integration & SSO endpoints, When accessed by authorized client, Then no 401 errors are observed and configuration is served")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testMoodleIntegrationEndpointsSuccess() throws Exception {
        // Get Moodle SSO configuration endpoint
        mockMvc.perform(get("/api/v1/auth/moodle/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_url").exists());

        // Get Moodle Role Hierarchy Mappings endpoint
        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isOk());
    }
}
