package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordRecoveryService passwordRecoveryService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    public record LoginRequest(String username, String password) {}
    public record RefreshTokenRequest(String refresh_token) {}
    public record LogoutRequest(String refresh_token) {}
    public record PasswordRecoveryRequest(String identity) {}
    public record PasswordResetConfirmationRequest(String recovery_token, String new_password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Необходимо указать имя пользователя и пароль.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        User user = userService.findByUsername(request.username()).orElse(null);
        if (user == null || !userService.verifyPassword(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_CREDENTIALS",
                    "message", "Неверное имя пользователя или пароль.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        String refreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        );

        Map<String, Object> response = Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", userInfo
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery/request")
    public ResponseEntity<?> requestRecovery(@RequestBody PasswordRecoveryRequest request) {
        if (request == null || request.identity() == null || request.identity().trim().isEmpty()) {
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
        if (request == null || request.recovery_token() == null || request.new_password() == null) {
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
}
