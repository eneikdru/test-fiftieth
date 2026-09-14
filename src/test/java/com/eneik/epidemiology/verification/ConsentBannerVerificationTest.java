package com.eneik.epidemiology.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentBannerVerificationTest {

    @Test
    @DisplayName("Given the frontend layer, When ConsentBanner.svelte is evaluated, Then it contains valid accessibility and structural UI attributes")
    void testConsentBannerComponentStructureAndAccessibility() throws IOException {
        Path sveltePath = Path.of("frontend/src/components/ConsentBanner.svelte");
        assertTrue(Files.exists(sveltePath), "ConsentBanner.svelte file must exist");

        String content = Files.readString(sveltePath);
        assertTrue(content.contains("role=\"dialog\""), "Consent banner must have role='dialog'");
        assertTrue(content.contains("aria-labelledby=\"consent-banner-title\""), "Consent banner must reference aria-labelledby");
        assertTrue(content.contains("aria-describedby=\"consent-banner-desc\""), "Consent banner must reference aria-describedby");
        assertTrue(content.contains("id=\"consent-accept-btn\""), "Consent banner must contain accept button ID");
        assertTrue(content.contains("id=\"consent-reject-btn\""), "Consent banner must contain reject button ID");
        assertTrue(content.contains("Настройки конфиденциальности"), "Consent banner heading must be in Russian");
        assertTrue(content.contains("Отклонить"), "Reject button label must be in Russian");
        assertTrue(content.contains("Принять"), "Accept button label must be in Russian");
    }

    @Test
    @DisplayName("Given index.html entrypoint, When ConsentBanner is integrated, Then the component mount point and lifecycle script are present")
    void testConsentBannerHtmlIntegration() throws IOException {
        Path htmlPath = Path.of("frontend/index.html");
        assertTrue(Files.exists(htmlPath), "index.html must exist");

        String content = Files.readString(htmlPath);
        assertTrue(content.contains("id=\"consent-banner-mount\""), "index.html must contain consent-banner-mount target container");
        assertTrue(content.contains("initConsentBanner()"), "index.html must initialize consent banner module");
        assertTrue(content.contains("ConsentBanner.svelte"), "index.html must fetch ConsentBanner.svelte for dynamic mounting");
    }
}
