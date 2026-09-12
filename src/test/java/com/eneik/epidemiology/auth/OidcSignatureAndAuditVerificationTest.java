package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class OidcSignatureAndAuditVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        if (userService.findByUsername("oidc_verified_user").isEmpty()) {
            userService.createUser("oidc_verified_user", "SecurePass123!", "oidc_verified@epidemiology-inst.ru", "ОИДС Пользователь", "RESEARCHER");
        }
    }

    @Test
    @DisplayName("Given an OIDC login request with a valid signed JWT, When AuthController.fetchOidcProfile processes it, Then signature is verified and claims are extracted")
    void testOidcLogin_ValidSignature_ExtractsClaimsAndAuthenticates() throws Exception {
        String validOidcToken = jwtTokenProvider.generateToken("oidc_verified_user", "Исследователь", "Лаборатория геномики", "EPID-101");

        String ssoPayload = String.format("{\"username\":\"oidc_verified_user\",\"oidc_token\":\"%s\"}", validOidcToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("oidc_verified_user")))
                .andExpect(jsonPath("$.user.role", is("RESEARCHER")))
                .andExpect(jsonPath("$.user.department", is("Лаборатория геномики")))
                .andExpect(jsonPath("$.user.courses", is("EPID-101")));
    }

    @Test
    @DisplayName("Given an OIDC login request with forged or invalid signature, When AuthController.fetchOidcProfile processes it, Then rejects with 401 Unauthorized")
    void testOidcLogin_ForgedOrInvalidSignature_Returns401Unauthorized() throws Exception {
        String invalidOidcToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJvaWRjX3ZlcmlmaWVkX3VzZXIiLCJyb2xlIjoiSU5WQUxJRCJ9.invalid_signature_hash";

        String ssoPayload = String.format("{\"username\":\"oidc_verified_user\",\"oidc_token\":\"%s\"}", invalidOidcToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")))
                .andExpect(jsonPath("$.message", is("Недействительный токен OIDC или имя пользователя.")));
    }

    @Test
    @DisplayName("Given an OIDC login request with invalid issuer or audience, When AuthController.fetchOidcProfile processes it, Then rejects with 401 Unauthorized")
    void testOidcLogin_InvalidIssuerOrAudience_Returns401Unauthorized() throws Exception {
        String invalidIssToken = jwtTokenProvider.generateToken("oidc_verified_user", "Исследователь", null, null, "https://invalid-issuer.org", "epidemiology_portal");

        String ssoPayload = String.format("{\"username\":\"oidc_verified_user\",\"oidc_token\":\"%s\"}", invalidIssToken);

        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_SSO_TOKEN")));
    }

    @Test
    @DisplayName("Given the coverage audit report, When evaluated, Then report structure contains requirements and gaps with explicit evidence fields")
    void testCoverageAuditReport_StructureAndEvidenceValid() throws Exception {
        File auditReportFile = new File("coverage-audit-report.json");
        assertTrue(auditReportFile.exists(), "coverage-audit-report.json must exist in root");

        JsonNode root = objectMapper.readTree(auditReportFile);
        assertTrue(root.has("requirements"), "Audit report must contain requirements field");
        assertTrue(root.has("gaps"), "Audit report must contain gaps field");

        JsonNode gapsNode = root.get("gaps");
        assertTrue(gapsNode.isArray(), "gaps must be an array");

        for (JsonNode gap : gapsNode) {
            assertTrue(gap.has("evidence"), "Each gap item in coverage audit report must contain evidence field");
            assertFalse(gap.get("evidence").asText().isBlank(), "Gap evidence field must not be blank");
        }
    }
}
