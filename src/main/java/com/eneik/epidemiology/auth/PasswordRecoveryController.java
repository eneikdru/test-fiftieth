package com.eneik.epidemiology.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recovery")
public class PasswordRecoveryController {

    public record PasswordRecoveryRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("identity") String identity) {}

    public record PasswordResetConfirmationRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("recovery_token") String recovery_token, @com.fasterxml.jackson.annotation.JsonProperty("new_password") String new_password) {}

    private final PasswordRecoveryService passwordRecoveryService;

    public PasswordRecoveryController(PasswordRecoveryService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/request")
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

    @PostMapping("/reset")
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
}
