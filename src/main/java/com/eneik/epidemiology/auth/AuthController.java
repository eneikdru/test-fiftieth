package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordRecoveryService passwordRecoveryService;
    private final com.eneik.epidemiology.telemetry.TelemetryService telemetryService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordRecoveryService passwordRecoveryService, com.eneik.epidemiology.telemetry.TelemetryService telemetryService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordRecoveryService = passwordRecoveryService;
        this.telemetryService = telemetryService;
    }

    public record RegistrationRequest(String username, String password, String email, String full_name) {}
    public record SsoLoginRequest(String username, String moodle_token) {}
    public record LoginRequest(String username, String password) {}
    public record RefreshTokenRequest(String refresh_token) {}
    public record LogoutRequest(String refresh_token) {}
    public record PasswordRecoveryRequest(String identity) {}
    public record PasswordResetConfirmationRequest(String recovery_token, String new_password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password()) || isBlank(request.email()) || isBlank(request.full_name())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Заполните все обязательные поля для регистрации.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        if (userService.existsByUsername(request.username().trim()) || userService.existsByEmail(request.email().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error_code", "USER_ALREADY_EXISTS",
                    "message", "Пользователь с таким именем или email уже существует.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        User user = userService.createUser(
                request.username().trim(),
                request.password(),
                request.email().trim(),
                request.full_name().trim(),
                "USER"
        );

        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Регистрация успешно завершена.",
                "user", buildUserInfo(user)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Необходимо указать имя пользователя и пароль.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        User user = userService.findByUsernameOrEmail(request.username().trim()).orElse(null);
        if (user == null || !userService.verifyPassword(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_CREDENTIALS",
                    "message", "Неверное имя пользователя или пароль.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        telemetryService.recordFallbackLoginTelemetry(user.getUsername());

        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        String refreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

        Map<String, Object> response = Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", buildUserInfo(user)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sso/moodle")
    public ResponseEntity<?> ssoLogin(@RequestBody SsoLoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.moodle_token())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Необходимо указать имя пользователя и SSO токен.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        // Minimal mock validation for SSO token to prevent arbitrary auth bypass.
        // In a real implementation, this would involve verifying an OAuth2/OIDC token or SAML assertion
        // against the Moodle identity provider's public keys.
        if (!"mock_valid_moodle_token".equals(request.moodle_token())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_SSO_TOKEN",
                    "message", "Недействительный токен SSO.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        User user = userService.findByUsernameOrEmail(request.username().trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_CREDENTIALS",
                    "message", "Пользователь не найден.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        telemetryService.recordSsoLoginTelemetry(user.getUsername());

        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        String refreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

        Map<String, Object> response = Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", buildUserInfo(user)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        if (request == null || isBlank(request.refresh_token())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Укажите refresh_token.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String token = request.refresh_token().trim();
        String username = extractUsernameFromRefreshToken(token);
        User user = userService.findByUsername(username).orElse(null);

        if (user == null || !token.startsWith("ref_")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_TOKEN",
                    "message", "Недействительный или просроченный токен обновления.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String newAccessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        String newRefreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

        Map<String, Object> response = Map.of(
                "access_token", newAccessToken,
                "refresh_token", newRefreshToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", buildUserInfo(user)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        if (request == null || isBlank(request.refresh_token())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Укажите refresh_token.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Успешный выход из системы."
        ));
    }

    @PostMapping("/recovery/request")
    public ResponseEntity<?> requestRecovery(@RequestBody PasswordRecoveryRequest request) {
        if (request == null || isBlank(request.identity())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_IDENTITY",
                    "message", "Укажите имя пользователя или адрес электронной почты.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        try {
            PasswordRecoveryService.RecoveryResponse response = passwordRecoveryService.initiateRecovery(request.identity().trim());
            return ResponseEntity.ok(Map.of(
                    "recovery_id", response.recoveryId().toString(),
                    "recovery_token", response.recoveryToken(),
                    "recovery_link", response.recoveryLink(),
                    "message", response.message()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error_code", "USER_NOT_FOUND",
                    "message", "Пользователь с указанной учетной записью не найден.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
    }

    @PostMapping("/recovery/reset")
    public ResponseEntity<?> confirmReset(@RequestBody PasswordResetConfirmationRequest request) {
        if (request == null || isBlank(request.recovery_token()) || isBlank(request.new_password())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Необходимо указать токен восстановления и новый пароль.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        try {
            passwordRecoveryService.confirmReset(request.recovery_token(), request.new_password());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Пароль успешно изменен."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error_code", "TOKEN_NOT_FOUND",
                    "message", e.getMessage(),
                    "timestamp", OffsetDateTime.now().toString()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "TOKEN_INVALID",
                    "message", e.getMessage(),
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        if (user.getFullName() != null) {
            map.put("full_name", user.getFullName());
        }
        if (user.getEmail() != null) {
            map.put("email", user.getEmail());
        }
        return map;
    }

    private static String extractUsernameFromRefreshToken(String token) {
        if (token != null && token.startsWith("ref_")) {
            int firstUnderscore = token.indexOf('_');
            int lastUnderscore = token.lastIndexOf('_');
            if (firstUnderscore >= 0 && lastUnderscore > firstUnderscore) {
                return token.substring(firstUnderscore + 1, lastUnderscore);
            }
        }
        return "";
    }
}
