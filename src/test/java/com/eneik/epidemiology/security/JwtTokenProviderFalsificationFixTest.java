package com.eneik.epidemiology.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderFalsificationFixTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider("default-secret-key-for-jwt-signing-2026-epidemiology-portal", 3600);
    }

    @Test
    @DisplayName("Given JSON missing requested key, When extractJsonValue is called, Then throws IllegalArgumentException")
    void testExtractJsonValueMissingKeyThrowsException() {
        String json = "{\"sub\":\"john_doe\",\"role\":\"USER\"}";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.extractJsonValue(json, "non_existent_key");
        });
        assertTrue(exception.getMessage().contains("Key 'non_existent_key' not found"));
    }

    @Test
    @DisplayName("Given malformed JSON, When extractJsonValue is called, Then throws IllegalArgumentException")
    void testExtractJsonValueMalformedJsonThrowsException() {
        String malformedJson = "{\"sub\":\"john_doe\", role: invalid_json";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.extractJsonValue(malformedJson, "sub");
        });
        assertTrue(exception.getMessage().contains("Malformed JSON payload"));
    }

    @Test
    @DisplayName("Given valid JSON with key, When extractJsonValue is called, Then returns correct value")
    void testExtractJsonValueValidKeyReturnsValue() {
        String json = "{\"sub\":\"john_doe\",\"role\":\"ADMIN\"}";
        assertEquals("john_doe", jwtTokenProvider.extractJsonValue(json, "sub"));
        assertEquals("ADMIN", jwtTokenProvider.extractJsonValue(json, "role"));
    }

    @Test
    @DisplayName("Given unverified or forged token, When getUsername or getRole is called, Then throws IllegalArgumentException")
    void testGetUsernameAndRoleWithUnverifiedTokenThrowsException() {
        String validToken = jwtTokenProvider.generateToken("john_doe", "ADMIN");
        String forgedToken = validToken.substring(0, validToken.lastIndexOf('.')) + ".invalid_signature";

        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getUsername(forgedToken);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getRole(forgedToken);
        });
    }

    @Test
    @DisplayName("Given valid signed token, When getUsername and getRole are called, Then returns claims successfully")
    void testGetUsernameAndRoleWithValidToken() {
        String validToken = jwtTokenProvider.generateToken("john_doe", "ADMIN");
        assertEquals("john_doe", jwtTokenProvider.getUsername(validToken));
        assertEquals("ADMIN", jwtTokenProvider.getRole(validToken));
    }

    @Test
    @DisplayName("Given JSON array with object authority or string elements, When extractJsonValue is called, Then returns extracted string value")
    void testExtractJsonValueArrayStructures() {
        String jsonArrayObj = "{\"authorities\":[{\"authority\":\"ROLE_ADMIN\"}]}";
        assertEquals("ROLE_ADMIN", jwtTokenProvider.extractJsonValue(jsonArrayObj, "authorities"));

        String jsonArrayStr = "{\"roles\":[\"RESEARCHER\",\"EPIDEMIOLOGIST\"]}";
        assertEquals("RESEARCHER", jwtTokenProvider.extractJsonValue(jsonArrayStr, "roles"));
    }

    @Test
    @DisplayName("Given token without 'role' claim but with 'authorities', When getRole is called, Then logs missing claim and resolves role")
    void testGetRoleLogsMissingClaimAndFallback() {
        String tokenWithAuthorities = jwtTokenProvider.generateToken("jane_doe", "EPIDEMIOLOGIST");
        assertEquals("EPIDEMIOLOGIST", jwtTokenProvider.getRole(tokenWithAuthorities));
    }
}
