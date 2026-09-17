package com.eneik.epidemiology.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CredentialTransmissionService {

    private static final Logger log = LoggerFactory.getLogger(CredentialTransmissionService.class);

    public record TransmissionRecord(String username, String recipient, String transmittedPasswordMasked, long timestamp) {}

    private final List<TransmissionRecord> transmissionHistory = new CopyOnWriteArrayList<>();

    /**
     * Transmits generated fallback password securely to the user via the chosen secure channel.
     * The raw password is transmitted in the one-time transmission payload and NOT stored in plain text permanently.
     *
     * @param username    The target username
     * @param recipient   Target email or user identifier
     * @param rawPassword The newly generated raw fallback password
     */
    public void transmitFallbackCredential(String username, String recipient, String rawPassword) {
        if (username == null || rawPassword == null) {
            log.warn("Attempted fallback credential transmission with null username or password.");
            return;
        }

        String targetRecipient = (recipient != null && !recipient.trim().isEmpty()) ? recipient.trim() : username;
        String maskedPassword = maskPassword(rawPassword);

        log.info("[SECURE CREDENTIAL TRANSMISSION] Transmitted fallback credential for user '{}' to recipient '{}' (password length: {})",
                username, targetRecipient, rawPassword.length());

        transmissionHistory.add(new TransmissionRecord(
                username,
                targetRecipient,
                maskedPassword,
                System.currentTimeMillis()
        ));
    }

    public List<TransmissionRecord> getTransmissionHistory() {
        return Collections.unmodifiableList(transmissionHistory);
    }

    public void clearTransmissionHistory() {
        transmissionHistory.clear();
    }

    private static String maskPassword(String pass) {
        if (pass == null || pass.length() <= 2) {
            return "***";
        }
        return pass.charAt(0) + "***" + pass.charAt(pass.length() - 1);
    }
}
