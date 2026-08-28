package com.eneik.epidemiology.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testAuthWithExpiredToken() throws Exception {
        // Create a custom JwtTokenProvider with a clock fixed in the past
        Clock pastClock = Clock.fixed(Instant.now().minusSeconds(10000), ZoneId.of("UTC"));
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                "default-secret-key-for-jwt-signing-2026-epidemiology-portal",
                3600, // valid for 1 hour from creation
                pastClock
        );
        String token = expiredTokenProvider.generateToken("user", "USER");

        mockMvc.perform(get("/api/v1/dossier/documents").header("Authorization", "Bearer " + token))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthWithValidToken() throws Exception {
        String token = jwtTokenProvider.generateToken("user", "USER");
        mockMvc.perform(get("/api/v1/dossier/documents").header("Authorization", "Bearer " + token))
               .andExpect(status().isOk());
    }
}
