package com.eneik.epidemiology.strain;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
public class StrainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StrainRepository strainRepository;

    private UUID deptStrainId;
    private UUID otherDeptStrainId;

    @BeforeEach
    void setUp() {
        strainRepository.deleteAll();
        userRepository.deleteAll();

        // Strains
        Strain strainOpen = new Strain();
        strainOpen.setId(UUID.randomUUID());
        strainOpen.setName("Open Strain");
        strainRepository.save(strainOpen);

        Strain strainDept = new Strain();
        deptStrainId = UUID.randomUUID();
        strainDept.setId(deptStrainId);
        strainDept.setName("Dept Strain");
        strainDept.setAccessDepartment("BIO");
        strainRepository.save(strainDept);

        Strain strainCourse = new Strain();
        strainCourse.setId(UUID.randomUUID());
        strainCourse.setName("Course Strain");
        strainCourse.setAccessCourse("BIO101");
        strainRepository.save(strainCourse);

        Strain strainOtherDept = new Strain();
        otherDeptStrainId = UUID.randomUUID();
        strainOtherDept.setId(otherDeptStrainId);
        strainOtherDept.setName("Other Dept Strain");
        strainOtherDept.setAccessDepartment("CHEM");
        strainRepository.save(strainOtherDept);

        // Users
        User userBio = new User(); userBio.setCreatedAt(java.time.OffsetDateTime.now());
        userBio.setUsername("userBio");
        userBio.setPasswordHash("hash");
        userBio.setRole("RESEARCHER");
        userBio.setDepartment("BIO");
        userBio.setCourses("BIO101,BIO102");
        userRepository.save(userBio);

        User userChem = new User(); userChem.setCreatedAt(java.time.OffsetDateTime.now());
        userChem.setUsername("userChem");
        userChem.setPasswordHash("hash");
        userChem.setRole("RESEARCHER");
        userChem.setDepartment("CHEM");
        userChem.setCourses("CHEM101");
        userRepository.save(userChem);

        User userAdmin = new User(); userAdmin.setCreatedAt(java.time.OffsetDateTime.now());
        userAdmin.setUsername("admin");
        userAdmin.setPasswordHash("hash");
        userAdmin.setRole("ADMIN");
        userRepository.save(userAdmin);
    }

    @Test
    @WithMockUser(username = "userBio")
    void shouldReturnStrainsMatchingDepartmentAndCourses() throws Exception {
        mockMvc.perform(get("/api/v1/strains"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(3)))
               .andExpect(jsonPath("$[?(@.name == 'Open Strain')]").exists())
               .andExpect(jsonPath("$[?(@.name == 'Dept Strain')]").exists())
               .andExpect(jsonPath("$[?(@.name == 'Course Strain')]").exists())
               .andExpect(jsonPath("$[?(@.name == 'Other Dept Strain')]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "userChem")
    void shouldOmitUnauthorizedStrains() throws Exception {
        mockMvc.perform(get("/api/v1/strains"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[?(@.name == 'Open Strain')]").exists())
               .andExpect(jsonPath("$[?(@.name == 'Other Dept Strain')]").exists())
               .andExpect(jsonPath("$[?(@.name == 'Dept Strain')]").doesNotExist())
               .andExpect(jsonPath("$[?(@.name == 'Course Strain')]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin")
    void adminShouldSeeAllStrains() throws Exception {
        mockMvc.perform(get("/api/v1/strains"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @WithMockUser(username = "userBio")
    void shouldCreateStrainWithAccessRules() throws Exception {
        String requestJson = """
                {
                    "name": "Ebola Zaire",
                    "description": "High consequence pathogen",
                    "originCountry": "Congo",
                    "severityLevel": "CRITICAL",
                    "accessDepartment": "BIO",
                    "accessCourse": "BIO102"
                }
                """;

        mockMvc.perform(post("/api/v1/strains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").exists())
               .andExpect(jsonPath("$.name").value("Ebola Zaire"))
               .andExpect(jsonPath("$.accessDepartment").value("BIO"))
               .andExpect(jsonPath("$.accessCourse").value("BIO102"));
    }

    @Test
    @WithMockUser(username = "userBio")
    void shouldModifyExistingStrainAndAccessRules() throws Exception {
        String updateJson = """
                {
                    "name": "Dept Strain Modified",
                    "description": "Updated description",
                    "accessDepartment": "BIO",
                    "accessCourse": "BIO101"
                }
                """;

        mockMvc.perform(put("/api/v1/strains/" + deptStrainId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(deptStrainId.toString()))
               .andExpect(jsonPath("$.name").value("Dept Strain Modified"))
               .andExpect(jsonPath("$.accessCourse").value("BIO101"));

        Strain updated = strainRepository.findById(deptStrainId).orElseThrow();
        assertEquals("Dept Strain Modified", updated.getName());
        assertEquals("BIO101", updated.getAccessCourse());
    }

    @Test
    @WithMockUser(username = "userBio")
    void shouldDenyUpdateForUnauthorizedStrain() throws Exception {
        String updateJson = """
                {
                    "name": "Illegal Modification Attempt",
                    "accessDepartment": "CHEM"
                }
                """;

        mockMvc.perform(put("/api/v1/strains/" + otherDeptStrainId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin")
    void shouldDeleteStrainById() throws Exception {
        mockMvc.perform(delete("/api/v1/strains/" + deptStrainId))
               .andExpect(status().isNoContent());

        assertTrue(strainRepository.findById(deptStrainId).isEmpty());
    }

    @Test
    @WithMockUser(username = "userBio")
    void shouldDenyDeleteForUnauthorizedStrain() throws Exception {
        mockMvc.perform(delete("/api/v1/strains/" + otherDeptStrainId))
               .andExpect(status().isForbidden());

        assertTrue(strainRepository.findById(otherDeptStrainId).isPresent());
    }
}
