package com.eneik.epidemiology.dataset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EpidemiologicalDatasetContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given the system initializes with a valid runtime configuration mapping epidemiological datasets, When the contract is loaded, Then it defines the required schema precisely.")
    @WithMockUser(username = "testuser")
    void testValidEpidemiologicalSchemaInteraction() throws Exception {
        String payload = """
                {
                    "datasetId": "SARS-CoV-2",
                    "schemaVersion": "v1.0",
                    "payload": {
                        "date": "2026-08-26"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/datasets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("valid"));
    }

    @Test
    @DisplayName("Given an invalid epidemiological schema interaction, When the contract validates it, Then it rejects the interaction with a structural error.")
    @WithMockUser(username = "testuser")
    void testInvalidEpidemiologicalSchemaInteraction() throws Exception {
        String payload = """
                {
                    "datasetId": "UNKNOWN-VIRUS",
                    "schemaVersion": "v1.0"
                }
                """;

        mockMvc.perform(post("/api/v1/datasets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Structural error: unknown datasetId"));
    }

    @Test
    @DisplayName("Given the system is under test, When unit integration tests verify the contract boundary, Then the schema enforces the expected bounds.")
    @WithMockUser(username = "testuser")
    void testInvalidSchemaVersionEnforcesBounds() throws Exception {
        String payload = """
                {
                    "datasetId": "SARS-CoV-2",
                    "schemaVersion": "v2.0"
                }
                """;

        mockMvc.perform(post("/api/v1/datasets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Structural error: unsupported schemaVersion"));
    }
}
