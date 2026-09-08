package com.eneik.epidemiology.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private final String secretKey;
    private final long accessTokenValidityInSeconds;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(
            @Value("${app.jwt.secret:default-secret-key-for-jwt-signing-2026-epidemiology-portal}") String secretKey,
            @Value("${app.jwt.expiration-seconds:3600}") long accessTokenValidityInSeconds) {
        this(secretKey, accessTokenValidityInSeconds, Clock.systemUTC());
    }

    public JwtTokenProvider(String secretKey, long accessTokenValidityInSeconds, Clock clock) {
        this.secretKey = secretKey;
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.clock = clock;
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
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return extractJsonValue(payload, "sub");
    }

    public String getRole(String token) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return extractJsonValue(payload, "role");
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

    private String extractJsonValue(String json, String key) {
        String keyPattern = "\"" + key + "\":";
        int idx = json.indexOf(keyPattern);
        if (idx == -1) {
            return "";
        }
        int start = idx + keyPattern.length();
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } else {
            int end = json.indexOf(',', start);
            if (end == -1) {
                end = json.indexOf('}', start);
            }
            return json.substring(start, end).trim();
        }
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
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
