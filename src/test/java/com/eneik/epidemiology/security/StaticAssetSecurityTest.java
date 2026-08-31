package com.eneik.epidemiology.security;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@AutoConfigureMockMvc
class StaticAssetSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given an unauthenticated request for static HTML, When evaluated by SecurityConfig, Then access is permitted with 200 OK")
    void testStaticHtmlAccess_PermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/registration-harness.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test-harness.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dossier-harness.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given an unauthenticated request for static asset files, When evaluated by SecurityConfig, Then access is permitted without 401 Unauthorized")
    void testStaticAssetsAccess_PermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/assets/logo.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given an unauthenticated request for API endpoints, When evaluated by SecurityConfig, Then returns 401 Unauthorized")
    void testApiEndpointAccess_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized());
    }
}
