package com.eneik.epidemiology.strain;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StrainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StrainRepository strainRepository;

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
        strainDept.setId(UUID.randomUUID());
        strainDept.setName("Dept Strain");
        strainDept.setAccessDepartment("BIO");
        strainRepository.save(strainDept);

        Strain strainCourse = new Strain();
        strainCourse.setId(UUID.randomUUID());
        strainCourse.setName("Course Strain");
        strainCourse.setAccessCourse("BIO101");
        strainRepository.save(strainCourse);

        Strain strainOtherDept = new Strain();
        strainOtherDept.setId(UUID.randomUUID());
        strainOtherDept.setName("Other Dept Strain");
        strainOtherDept.setAccessDepartment("CHEM");
        strainRepository.save(strainOtherDept);

        // Users
        User userBio = new User();
        userBio.setUsername("userBio");
        userBio.setPasswordHash("hash");
        userBio.setRole("RESEARCHER");
        userBio.setDepartment("BIO");
        userBio.setCourses("BIO101,BIO102");
        userRepository.save(userBio);

        User userChem = new User();
        userChem.setUsername("userChem");
        userChem.setPasswordHash("hash");
        userChem.setRole("RESEARCHER");
        userChem.setDepartment("CHEM");
        userChem.setCourses("CHEM101");
        userRepository.save(userChem);

        User userAdmin = new User();
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
}
