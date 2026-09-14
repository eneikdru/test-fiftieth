package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.auth.AuthController;
import com.eneik.epidemiology.strain.Strain;
import com.eneik.epidemiology.strain.StrainRepository;
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

import java.util.UUID;

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
class MoodleToCatalogJourneyVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StrainRepository strainRepository;

    @Autowired
    private AuthController authController;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(authController.getRestTemplate());

        // Seed sample strain for catalog search verification
        Strain strain = new Strain();
        strain.setId(UUID.randomUUID());
        strain.setName("Salmonella enterica");
        strain.setDescription("Epidemiological strain sample");
        strain.setOriginCountry("Russia");
        strain.setSeverityLevel("MEDIUM");
        strain.setAccessDepartment("EPIDEMIOLOGY");
        strainRepository.save(strain);
    }

    @Test
    @DisplayName("Given Moodle SSO login, When user authenticates and searches catalog, Then data is returned seamlessly without 401 errors")
    void testFullMoodleLoginToCatalogSearchUserJourney() throws Exception {
        // Mock external Moodle userinfo endpoint response
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer valid_moodle_token_123"))
                .andRespond(withSuccess(
                        "{\"username\":\"moodle_epidemiologist\",\"moodle_role\":\"Эпидемиолог\",\"department\":\"EPIDEMIOLOGY\",\"email\":\"moodle_ep@inst.ru\",\"full_name\":\"Moodle Epidemiologist\",\"courses\":\"EPI101\"}",
                        MediaType.APPLICATION_JSON
                ));

        String ssoRequestBody = """
                {
                    "username": "moodle_epidemiologist",
                    "moodle_token": "valid_moodle_token_123"
                }
                """;

        // 1. Authenticate via Moodle SSO
        String responseContent = mockMvc.perform(post("/api/v1/auth/sso/moodle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ssoRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("moodle_epidemiologist")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")))
                .andReturn().getResponse().getContentAsString();

        // Extract JWT access token from response
        String accessToken = responseContent.split("\"access_token\":\"")[1].split("\"")[0];

        // 2. Perform catalog search with authenticated JWT token
        mockMvc.perform(get("/api/v1/strains")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Salmonella enterica')]").exists());
    }

    @Test
    @DisplayName("Given backend API SSO endpoints, When accessed by client, Then they succeed without 401 regressions")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUpdatedSsoEndpointsWithout401Regressions() throws Exception {
        // GET /api/v1/auth/moodle/config
        mockMvc.perform(get("/api/v1/auth/moodle/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_url", notNullValue()));

        // GET /api/v1/auth/moodle/override-role
        mockMvc.perform(get("/api/v1/auth/moodle/override-role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings", notNullValue()));

        // POST /api/v1/auth/moodle/override-role
        String overrideJson = """
                {
                    "moodle_role_pattern": "исследователь",
                    "internal_role": "RESEARCHER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/moodle/override-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overrideJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
