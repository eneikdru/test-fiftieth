package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);

    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository recoveryTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Random random;
    private final String baseUrl;

    @Autowired
    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordRecoveryTokenRepository recoveryTokenRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.recovery.base-url:http://localhost:8080}") String baseUrl) {
        this(userRepository, recoveryTokenRepository, passwordEncoder, Clock.systemUTC(), new SecureRandom(), baseUrl);
    }

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordRecoveryTokenRepository recoveryTokenRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            Random random,
            String baseUrl) {
        this.userRepository = userRepository;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.random = random != null ? random : new SecureRandom();
        this.baseUrl = baseUrl;
    }

    public record RecoveryResponse(UUID recoveryId, String recoveryToken, String recoveryLink, String message) {}

    @Transactional
    public RecoveryResponse initiateRecovery(String identity) {
        User user = userRepository.findByUsernameOrEmail(identity, identity)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с указанными данными не найден"));

        // Deterministic token string using seedable Random instance
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        String tokenValue = "rec_tok_" + bytesToHex(randomBytes);

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plusHours(1);

        PasswordRecoveryToken recoveryToken = new PasswordRecoveryToken(user, tokenValue, now, expiresAt);
        recoveryTokenRepository.save(recoveryToken);

        String recoveryLink = baseUrl + "/reset-password?token=" + tokenValue;
        UUID recoveryId = UUID.nameUUIDFromBytes(tokenValue.getBytes());

        // Outbound notification channel simulation (e.g. email/SMS dispatcher)
        log.info("[OUTBOUND NOTIFICATION CHANNEL] Sent recovery link '{}' to identity '{}'", recoveryLink, identity);

        String message = "Инструкции по восстановлению пароля отправлены на ваш электронный адрес.";

        return new RecoveryResponse(recoveryId, tokenValue, recoveryLink, message);
    }

    @Transactional
    public void confirmReset(String recoveryToken, String newPassword) {
        PasswordRecoveryToken tokenEntity = recoveryTokenRepository.findByToken(recoveryToken)
                .orElseThrow(() -> new IllegalArgumentException("Токен восстановления не найден"));

        if (tokenEntity.isUsed()) {
            throw new IllegalStateException("Токен восстановления уже использован");
        }

        if (tokenEntity.getExpiresAt().isBefore(OffsetDateTime.now(clock))) {
            throw new IllegalStateException("Срок действия токена восстановления истек");
        }

        // Atomically guard update to prevent read-then-save races
        int updatedRows = recoveryTokenRepository.markAsUsedAtomically(tokenEntity.getId());
        if (updatedRows == 0) {
            throw new IllegalStateException("Токен восстановления уже использован или заблокирован");
        }

        User user = tokenEntity.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
