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
}
