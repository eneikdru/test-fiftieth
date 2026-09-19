package com.eneik.epidemiology.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final String secretKey;
    private final long accessTokenValidityInSeconds;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtTokenProvider(
            @Value("${app.jwt.secret:default-secret-key-for-jwt-signing-2026-epidemiology-portal}") String secretKey,
            @Value("${app.jwt.expiration-seconds:3600}") long accessTokenValidityInSeconds) {
        this(secretKey, accessTokenValidityInSeconds, Clock.systemUTC(), new ObjectMapper());
    }

    public JwtTokenProvider(String secretKey, long accessTokenValidityInSeconds, Clock clock) {
        this(secretKey, accessTokenValidityInSeconds, clock, new ObjectMapper());
    }

    public JwtTokenProvider(String secretKey, long accessTokenValidityInSeconds, Clock clock, ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.clock = clock;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, null, null);
    }

    public String generateRefreshToken(String username) {
        Instant now = clock.instant();
        Instant exp = now.plusSeconds(30L * 24 * 3600); // 30 days

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        StringBuilder payloadBuilder = new StringBuilder();
        payloadBuilder.append(String.format(
                "{\"sub\":\"%s\",\"type\":\"refresh\"",
                escapeJson(username)
        ));
        payloadBuilder.append(String.format(",\"iat\":%d,\"exp\":%d}", now.getEpochSecond(), exp.getEpochSecond()));

        String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payloadBuilder.toString().getBytes(StandardCharsets.UTF_8));

        String contentToSign = encodedHeader + "." + encodedPayload;
        String signature = hmacSha256(contentToSign, secretKey);

        return contentToSign + "." + signature;
    }

    public String generateToken(String username, String role, String department, String courses) {
        return generateToken(username, role, department, courses, "https://moodle.epidemiology-inst.ru", "epidemiology_portal");
    }

    public String generateToken(String username, String role, String department, String courses, String issuer, String audience) {
        Instant now = clock.instant();
        Instant exp = now.plusSeconds(accessTokenValidityInSeconds);

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        StringBuilder payloadBuilder = new StringBuilder();
        payloadBuilder.append(String.format(
                "{\"sub\":\"%s\",\"role\":\"%s\"",
                escapeJson(username),
                escapeJson(role)
        ));
        if (department != null && !department.trim().isEmpty()) {
            payloadBuilder.append(String.format(",\"department\":\"%s\"", escapeJson(department.trim())));
        }
        if (courses != null && !courses.trim().isEmpty()) {
            payloadBuilder.append(String.format(",\"courses\":\"%s\"", escapeJson(courses.trim())));
        }
        if (issuer != null && !issuer.trim().isEmpty()) {
            payloadBuilder.append(String.format(",\"iss\":\"%s\"", escapeJson(issuer.trim())));
        }
        if (audience != null && !audience.trim().isEmpty()) {
            payloadBuilder.append(String.format(",\"aud\":\"%s\"", escapeJson(audience.trim())));
        }
        payloadBuilder.append(String.format(",\"iat\":%d,\"exp\":%d}", now.getEpochSecond(), exp.getEpochSecond()));

        String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payloadBuilder.toString().getBytes(StandardCharsets.UTF_8));

        String contentToSign = encodedHeader + "." + encodedPayload;
        String signature = hmacSha256(contentToSign, secretKey);

        return contentToSign + "." + signature;
    }

    public boolean validateToken(String token) {
        if (token == null || !token.contains(".")) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String contentToSign = parts[0] + "." + parts[1];
        String expectedSignature = hmacSha256(contentToSign, secretKey);

        if (!expectedSignature.equals(parts[2])) {
            return false;
        }

        long exp = extractExpiration(parts[1]);
        if (exp == -1) {
            return false;
        }

        return clock.instant().getEpochSecond() <= exp;
    }

    public String getUsername(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Invalid or unverified JWT token signature or expiration");
        }
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        try {
            return extractJsonValue(payload, "sub");
        } catch (IllegalArgumentException e1) {
            try {
                return extractJsonValue(payload, "username");
            } catch (IllegalArgumentException e2) {
                return extractJsonValue(payload, "preferred_username");
            }
        }
    }

    public String getRole(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Invalid or unverified JWT token signature or expiration");
        }
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String[] claimKeys = new String[]{"role", "roles", "authorities"};
        for (String key : claimKeys) {
            try {
                String val = extractJsonValue(payload, key);
                if (val != null && !val.trim().isEmpty()) {
                    return val;
                }
            } catch (IllegalArgumentException e) {
                log.debug("Claim key '{}' not present or invalid in token payload: {}", key, e.getMessage());
            }
        }
        return null;
    }

    private long extractExpiration(String base64Payload) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(base64Payload), StandardCharsets.UTF_8);
            String expStr = extractJsonValue(payload, "exp");
            return Long.parseLong(expStr);
        } catch (Exception e) {
            return -1;
        }
    }

    public String extractJsonValue(String json, String key) {
        JsonNode tree;
        try {
            tree = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed JSON payload: " + e.getMessage(), e);
        }

        if (tree == null || !tree.has(key) || tree.get(key).isNull()) {
            throw new IllegalArgumentException("Key '" + key + "' not found in JSON payload");
        }

        JsonNode node = tree.get(key);
        if (!node.isArray() || node.isEmpty()) {
            return node.asText();
        }

        JsonNode first = node.get(0);
        if (!first.isObject()) {
            return first.asText();
        }

        if (first.has("authority") && !first.get("authority").isNull()) {
            return first.get("authority").asText();
        }
        if (first.has("role") && !first.get("role").isNull()) {
            return first.get("role").asText();
        }

        return first.asText();
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error signing JWT token", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
