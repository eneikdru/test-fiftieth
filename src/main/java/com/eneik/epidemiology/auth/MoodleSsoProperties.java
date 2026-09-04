package com.eneik.epidemiology.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "moodle.oauth2")
public class MoodleSsoProperties {
    private String clientId;
    private String clientSecret;
    private String tokenUri = "https://moodle.epidemiology-inst.ru/login/token.php";
    private String userInfoUri = "https://moodle.epidemiology-inst.ru/webservice/rest/server.php";
    private String authorizationUri = "https://moodle.epidemiology-inst.ru/local/oauth/login.php";
    private String redirectUri = "http://localhost:3000/auth/moodle/callback";

    @jakarta.annotation.PostConstruct
    public void validate() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException("Moodle OAuth2 client ID must be configured");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalStateException("Moodle OAuth2 client secret must be configured");
        }
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getTokenUri() { return tokenUri; }
    public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }
    public String getUserInfoUri() { return userInfoUri; }
    public void setUserInfoUri(String userInfoUri) { this.userInfoUri = userInfoUri; }
    public String getAuthorizationUri() { return authorizationUri; }
    public void setAuthorizationUri(String authorizationUri) { this.authorizationUri = authorizationUri; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
}
