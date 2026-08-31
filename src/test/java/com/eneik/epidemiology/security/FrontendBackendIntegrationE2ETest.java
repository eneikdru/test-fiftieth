package com.eneik.epidemiology.security;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@AutoConfigureMockMvc
public class FrontendBackendIntegrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given the frontend static resources served by Spring Boot, When static pages are requested, Then 200 OK is returned with HTML content")
    void testFrontendPagesServedSuccessfully() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/registration-harness.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test-harness.html"))
                .andExpect(status().isOk());
    }

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("Given the live system, When frontend catalog search endpoint is invoked, Then real document API responds without error")
    void testCatalogSearchApiEndpointIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").exists())
                .andExpect(jsonPath("$.results").isArray());
    }
}
