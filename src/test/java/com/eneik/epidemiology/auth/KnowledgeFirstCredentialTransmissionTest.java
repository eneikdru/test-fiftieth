package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
class KnowledgeFirstCredentialTransmissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordRecoveryTokenRepository tokenRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class RestTemplateConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.web.client.RestTemplate restTemplate() {
            return new org.springframework.web.client.RestTemplate();
        }
    }

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @Test
    @DisplayName("Given a fallback account creation event, When verified, Then the test must confirm a secure transmission token is generated for the user")
    void testFallbackAccountCreationGeneratesTransmissionToken() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer mock_valid_new_moodle_token"))
                .andRespond(withSuccess(
                        "{\"username\":\"transmission_user\",\"moodle_role\":\"Пользователь\",\"email\":\"transmission@inst.ru\"}",
                        MediaType.APPLICATION_JSON));

        // Inject fallback_password so ssoLogin bypasses validation via AuthControllerTest pattern
        // The endpoint is what triggers the fallback. If we provide fallback_password, we saw it worked in AuthControllerTest.testSsoLogin_NewUserAutoProvisioning.
        // Wait, AuthControllerTest uses a special internal bypass for mock_valid_new_moodle_token inside the controller's signature validation logic?
        // Actually, AuthController uses JWK for signature validation, and fails.
        // Let's just create the user mimicking the fallback account creation directly via the controller by simulating a direct registration, or just testing the underlying UserService, but wait, the fallback is in AuthController.

        org.junit.jupiter.api.Assertions.fail("Falsification Audit: Knowledge-First Credential Transmission is not implemented. AuthController generates fallback password without emitting a secure one-time link token.");
    }
}
