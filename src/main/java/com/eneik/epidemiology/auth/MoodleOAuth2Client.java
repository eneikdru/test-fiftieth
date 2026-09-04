package com.eneik.epidemiology.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MoodleOAuth2Client {

    private static final Logger log = LoggerFactory.getLogger(MoodleOAuth2Client.class);

    private final MoodleSsoProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public MoodleOAuth2Client(MoodleSsoProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    MoodleOAuth2Client(MoodleSsoProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public String getAuthorizationUrl() {
        return properties.getAuthorizationUri() +
                "?client_id=" + properties.getClientId() +
                "&response_type=code" +
                "&redirect_uri=" + properties.getRedirectUri();
    }

    public MoodleProfile exchangeCodeForProfile(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", properties.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getTokenUri(),
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> tokenData = response.getBody();
            if (tokenData == null || !tokenData.containsKey("access_token")) {
                return null;
            }

            String accessToken = (String) tokenData.get("access_token");
            return fetchProfile(accessToken);
        } catch (Exception e) {
            log.error("Failed to exchange authorization code for Moodle token", e);
            return null;
        }
    }

    private MoodleProfile fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    properties.getUserInfoUri(),
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> userData = response.getBody();
            if (userData == null) return null;

            return new MoodleProfile(
                (String) userData.get("username"),
                (String) userData.get("moodleRole"),
                (String) userData.get("department"),
                (String) userData.get("email"),
                (String) userData.get("fullName"),
                (String) userData.get("courses")
            );
        } catch (Exception e) {
            log.error("Failed to fetch Moodle user profile", e);
            return null;
        }
    }
}
