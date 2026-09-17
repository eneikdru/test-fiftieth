package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class CredentialTransmissionQaVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordRecoveryTokenRepository recoveryTokenRepository;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @Autowired
    private UserService.PasswordEncoderConfig passwordEncoderConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        recoveryTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Given a fallback account creation event, When password recovery transmission is triggered, Then recovery service generates and stores verifiable transmission link")
    void testFallbackAccountCredentialTransmission_InvokesRecoveryService() throws Exception {
        // Given fallback user created
        User user = userService.createUserWithMoodle(
                "fallback_sso_user",
                "GeneratedPass123!",
                "fallback_sso@epidemiology-inst.ru",
                "Fallback SSO User",
                "RESEARCHER",
                "moodle_sso_101",
                "Эпидемиология",
                "EPID-101"
        );

        // Fixed clock and seedable random for deterministic test outcome
        Instant fixedInstant = Instant.parse("2026-09-17T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        Random fixedRandom = new Random(987654321L);

        PasswordRecoveryService seedableRecoveryService = new PasswordRecoveryService(
                userRepository,
                recoveryTokenRepository,
                passwordEncoderConfig.passwordEncoder(),
                fixedClock,
                fixedRandom,
                "http://localhost:8080"
        );

        // When initiating recovery for the fallback identity
        PasswordRecoveryService.RecoveryResponse response = seedableRecoveryService.initiateRecovery("fallback_sso@epidemiology-inst.ru");

        // Then transmission credential/link is generated and returned
        assertNotNull(response);
        assertNotNull(response.recoveryToken());
        assertTrue(response.recoveryToken().startsWith("rec_tok_"));
        assertTrue(response.recoveryLink().contains("/reset-password?token=" + response.recoveryToken()));
        assertEquals("Инструкции по восстановлению пароля отправлены на ваш электронный адрес.", response.message());

        // Assert token entity was saved in repository for user
        PasswordRecoveryToken tokenEntity = recoveryTokenRepository.findByToken(response.recoveryToken()).orElse(null);
        assertNotNull(tokenEntity);
        assertEquals(user.getId(), tokenEntity.getUser().getId());
    }

    @Test
    @DisplayName("Given a successful credential transmission, When user confirms reset with token, Then user gains identity access via new credentials")
    void testFallbackAccountCredentialTransmission_UserGainsKnowledgeAndAccess() throws Exception {
        // Given user created
        User user = userService.createUser("epidem_user", "OldSecretPass123!", "epidem_user@inst.ru", "Эпидемиолог Иванов", "EPIDEMIOLOGIST");

        // Step 1: Trigger password recovery request endpoint
        String requestBody = "{\"identity\":\"epidem_user@inst.ru\"}";
        MvcResult recoveryResult = mockMvc.perform(post("/api/v1/auth/recovery/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recovery_token", notNullValue()))
                .andExpect(jsonPath("$.recovery_link", notNullValue()))
                .andReturn();

        String responseJson = recoveryResult.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
        String recoveryToken = (String) responseMap.get("recovery_token");

        // Step 2: Confirm password reset using the transmitted credential token
        String newPassword = "NewVerifiedKnowledgePassword2026!";
        String resetBody = String.format("{\"recovery_token\":\"%s\",\"new_password\":\"%s\"}", recoveryToken, newPassword);

        mockMvc.perform(post("/api/v1/auth/recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Пароль успешно изменен.")));

        // Step 3: Login with the newly acquired credentials to verify identity access
        String loginBody = String.format("{\"username\":\"epidem_user\",\"password\":\"%s\"}", newPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("epidem_user")))
                .andReturn();

        String loginJson = loginResult.getResponse().getContentAsString();
        Map<?, ?> loginMap = objectMapper.readValue(loginJson, Map.class);
        String accessToken = (String) loginMap.get("access_token");

        // Step 4: Access protected profile endpoint to confirm full epistemic link and identity access
        mockMvc.perform(get("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("epidem_user")))
                .andExpect(jsonPath("$.user.email", is("epidem_user@inst.ru")));
    }

    @Test
    @DisplayName("Given an authenticated fallback user, When profile is patched with new password knowledge, Then credential knowledge is confirmed and local login succeeds")
    void testProfileFallbackPasswordUpdate_ConfirmsCredentialKnowledge() throws Exception {
        // Given fallback user
        User user = userService.createUser("profile_fallback_user", "RandomInitialSecret!", "profile_fallback@inst.ru", "Профиль Текстов", "USER");

        // Initiate recovery to obtain token
        PasswordRecoveryService.RecoveryResponse recovery = passwordRecoveryService.initiateRecovery("profile_fallback_user");

        // Confirm reset with initial explicit password
        passwordRecoveryService.confirmReset(recovery.recoveryToken(), "ExplicitKnowledgePass2026!");

        // Login to get access token
        String loginBody = "{\"username\":\"profile_fallback_user\",\"password\":\"ExplicitKnowledgePass2026!\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = (String) objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class).get("access_token");

        // Patch profile with updated fallback password
        String patchBody = "{\"fallback_password\":\"UpdatedProfilePassword2026!\"}";
        mockMvc.perform(patch("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Authenticate locally using updated password
        String updatedLoginBody = "{\"username\":\"profile_fallback_user\",\"password\":\"UpdatedProfilePassword2026!\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedLoginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("profile_fallback_user")));
    }
}
