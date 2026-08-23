package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordRecoveryTokenRepository recoveryTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    @DisplayName("Given a user requests password reset with valid identity, When recovery endpoint is called, Then secure recovery link is generated and sent")
    void testRequestPasswordRecovery_GeneratesSecureRecoveryLink() throws Exception {
        User user = userService.createUser("petrov_sm", "Pass12345!", "RESEARCHER");

        // Verify service logic with explicit fixed clock and seedable random
        Instant fixedInstant = Instant.parse("2026-08-22T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        Random fixedRandom = new Random(12345L);

        PasswordRecoveryService customRecoveryService = new PasswordRecoveryService(
                userRepository,
                recoveryTokenRepository,
                passwordEncoderConfig.passwordEncoder(),
                fixedClock,
                fixedRandom,
                "http://localhost:8080"
        );

        PasswordRecoveryService.RecoveryResponse response = customRecoveryService.initiateRecovery("petrov_sm");

        assert response.recoveryLink().contains("/reset-password?token=rec_tok_");
        assert response.message().equals("Инструкции по восстановлению пароля отправлены на ваш электронный адрес.");

        // Execute via MockMvc
        String requestBody = "{\"identity\":\"petrov_sm\"}";

        mockMvc.perform(post("/api/v1/auth/recovery/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recovery_id", notNullValue()))
                .andExpect(jsonPath("$.recovery_token", notNullValue()))
                .andExpect(jsonPath("$.recovery_link", notNullValue()))
                .andExpect(jsonPath("$.message", is("Инструкции по восстановлению пароля отправлены на ваш электронный адрес.")));
    }

    @Test
    @DisplayName("Given a valid password reset token, When reset endpoint is called, Then user password is updated")
    void testConfirmPasswordReset_ResetsUserPassword() throws Exception {
        User user = userService.createUser("sidorov_v", "OldPassword1!", "USER");

        String requestBody = "{\"identity\":\"sidorov_v\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/recovery/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);
        String recoveryToken = (String) responseMap.get("recovery_token");

        String resetBody = String.format(
                "{\"recovery_token\":\"%s\",\"new_password\":\"NewStrongPass2026!\"}",
                recoveryToken
        );

        mockMvc.perform(post("/api/v1/auth/recovery/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Пароль успешно изменен.")));

        User updatedUser = userService.findByUsername("sidorov_v").orElseThrow();
        assert userService.verifyPassword("NewStrongPass2026!", updatedUser.getPasswordHash());
    }

    @Test
    @DisplayName("Given valid login credentials, When login endpoint called, Then returns JWT tokens and user info")
    void testLogin_ReturnsAuthTokens() throws Exception {
        userService.createUser("katya_exp", "KatyaPass123!", "RESEARCHER");

        String loginBody = "{\"username\":\"katya_exp\",\"password\":\"KatyaPass123!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("katya_exp")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")));
    }
}
