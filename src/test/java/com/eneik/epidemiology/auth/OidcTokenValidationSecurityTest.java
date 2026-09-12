package com.eneik.epidemiology.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
public class OidcTokenValidationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Value("${moodle.client.secret:}")
    private String moodleClientSecret;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.secret:default-secret-key-for-jwt-signing-2026-epidemiology-portal}")
    private String jwtSecret;

    private String createOidcToken(String iss, String aud, String secretKey) throws Exception {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format("{\"sub\":\"user\",\"iss\":\"%s\",\"aud\":\"%s\",\"exp\":%d}",
                iss, aud, (System.currentTimeMillis() / 1000) + 3600);

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String contentToSign = encodedHeader + "." + encodedPayload;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(contentToSign.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);

        return contentToSign + "." + signature;
    }

    @Test
    @DisplayName("Invalid issuer is rejected")
    void testInvalidIssuer() throws Exception {
        String token = createOidcToken("https://bad-issuer.com", "epidemiology_portal", jwtSecret);

        String ssoBody = "{\"username\":\"user\",\"oidc_token\":\"" + token + "\",\"fallback_password\":\"pass\"}";
        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid audience is rejected")
    void testInvalidAudience() throws Exception {
        String token = createOidcToken("https://moodle.epidemiology-inst.ru", "bad_client", jwtSecret);

        String ssoBody = "{\"username\":\"user\",\"oidc_token\":\"" + token + "\",\"fallback_password\":\"pass\"}";
        mockMvc.perform(post("/api/v1/auth/sso/oidc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isUnauthorized());
    }
}
