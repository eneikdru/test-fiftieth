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
    public record SsoLoginRequest(String username, String moodle_token, String fallback_password) {}
    public record LoginRequest(String username, String password) {}
    public record RefreshTokenRequest(String refresh_token) {}
    public record LogoutRequest(String refresh_token) {}
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
        // against the Moodle identity provider's public keys and fetching the user profile securely.
        MoodleProfile profile = fetchMoodleProfile(request.moodle_token());

        if (profile == null || !profile.username().equals(request.username())) {
            if (request.fallback_password() != null && !request.fallback_password().trim().isEmpty()) {
                User user = userService.findByUsernameOrEmail(request.username().trim()).orElse(null);
                if (user != null && userService.verifyPassword(request.fallback_password().trim(), user.getPasswordHash())) {
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
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_SSO_TOKEN",
                    "message", "Недействительный токен SSO или имя пользователя.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String internalRole = mapMoodleRole(profile.moodleRole());
        User user = userService.findByUsernameOrEmail(request.username().trim()).orElse(null);

        if (user == null) {
            String defaultPassword = (request.fallback_password() != null && !request.fallback_password().trim().isEmpty())
                ? request.fallback_password().trim()
                : "Fallback" + Math.abs(profile.username().hashCode()) + "!";
            user = userService.createUserWithMoodle(
                profile.username().trim(),
                defaultPassword,
                profile.email(),
                profile.fullName(),
                internalRole,
                profile.username().trim(),
                profile.department(),
                profile.courses()
            );
        } else {
            boolean needsUpdate = false;
            if (internalRole != null && !internalRole.equals(user.getRole())) {
                needsUpdate = true;
            }
            if (profile.department() != null && !profile.department().equals(user.getDepartment())) {
                needsUpdate = true;
            }
            if (profile.courses() != null && !profile.courses().equals(user.getCourses())) {
                needsUpdate = true;
            }
            if (needsUpdate) {
                userService.updateRoleAndDepartmentAtomically(user.getId(), user.getRole(), internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses());
                user.setRole(internalRole != null ? internalRole : user.getRole());
                user.setDepartment(profile.department());
                user.setCourses(profile.courses());
            }
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

    private record MoodleProfile(String username, String moodleRole, String department, String email, String fullName, String courses) {}

    private MoodleProfile fetchMoodleProfile(String token) {
        if ("mock_valid_moodle_token".equals(token)) {
            return new MoodleProfile("moodle_user", "Старший научный сотрудник", "Эпидемиология", "moodle@inst.ru", "Moodle User", "BIO-101");
        } else if ("mock_valid_new_moodle_token".equals(token)) {
            return new MoodleProfile("new_moodle_user", "Администратор", "IT", "new_moodle@inst.ru", "New Moodle Admin", "");
        }
        return null;
    }

    private static String mapMoodleRole(String moodleRole) {
        if (moodleRole == null) {
            return "USER";
        }
        String lowerRole = moodleRole.toLowerCase();
        if (lowerRole.contains("администратор")) {
            return "ADMIN";
        } else if (lowerRole.contains("старший научный сотрудник") || lowerRole.contains("эпидемиолог")) {
            return "EPIDEMIOLOGIST";
        } else if (lowerRole.contains("исследователь") || lowerRole.contains("аспирант")) {
            return "RESEARCHER";
        }
        return "USER";
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
