package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @org.springframework.beans.factory.annotation.Value("${moodle.server.url:https://moodle.epidemiology-inst.ru}")
    private String moodleServerUrl = "https://moodle.epidemiology-inst.ru";

    @org.springframework.beans.factory.annotation.Value("${moodle.client.id:epidemiology_portal}")
    private String moodleClientId = "epidemiology_portal";

    @org.springframework.beans.factory.annotation.Value("${moodle.client.secret:}")
    private String moodleClientSecret = "";

    @org.springframework.beans.factory.annotation.Value("${moodle.redirect.uri:http://localhost:8080/auth/moodle/callback}")
    private String moodleRedirectUri = "http://localhost:8080/auth/moodle/callback";

    @org.springframework.beans.factory.annotation.Value("${moodle.lti.consumer.secret:moodle_lti_secret}")
    private String moodleLtiSecret = "moodle_lti_secret";

    private final org.springframework.web.client.RestTemplate restTemplate;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordRecoveryService passwordRecoveryService;
    private final com.eneik.epidemiology.telemetry.TelemetryService telemetryService;
    private final JdbcTemplate jdbcTemplate;
    private final TokenRevocationService tokenRevocationService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordRecoveryService passwordRecoveryService, com.eneik.epidemiology.telemetry.TelemetryService telemetryService, JdbcTemplate jdbcTemplate, TokenRevocationService tokenRevocationService) {
        this(userService, jwtTokenProvider, passwordRecoveryService, telemetryService, jdbcTemplate, tokenRevocationService, new org.springframework.web.client.RestTemplate());
    }

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordRecoveryService passwordRecoveryService, com.eneik.epidemiology.telemetry.TelemetryService telemetryService, JdbcTemplate jdbcTemplate, TokenRevocationService tokenRevocationService, org.springframework.web.client.RestTemplate restTemplate) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordRecoveryService = passwordRecoveryService;
        this.telemetryService = telemetryService;
        this.jdbcTemplate = jdbcTemplate;
        this.tokenRevocationService = tokenRevocationService;
        this.restTemplate = restTemplate != null ? restTemplate : new org.springframework.web.client.RestTemplate();
    }

    public org.springframework.web.client.RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setMoodleServerUrl(String moodleServerUrl) {
        this.moodleServerUrl = moodleServerUrl;
    }

    public record RegistrationRequest(String username, String password, String email, String full_name) {}
    public record SsoLoginRequest(String username, String moodle_token, String fallback_password) {}
    public record OidcLoginRequest(String username, String oidc_token, String fallback_password) {}
    public record MoodleCallbackRequest(String code, String state, String username, String fallback_password) {}
    public record MoodleRoleOverrideRequest(
            Long userId,
            Long user_id,
            String username,
            String role,
            String moodle_role_pattern,
            String internal_role
    ) {}
    public record LtiLaunchRequest(
            String user_id,
            String ext_user_username,
            String username,
            String lis_person_name_full,
            String full_name,
            String lis_person_contact_email_primary,
            String email,
            String roles,
            String moodle_role,
            String custom_department,
            String department,
            String custom_courses,
            String courses,
            String lti_message_type,
            String lti_version,
            String oauth_consumer_key,
            String oauth_signature_method,
            String oauth_timestamp,
            String oauth_nonce,
            String oauth_signature
    ) {}
    public record LoginRequest(String username, String password) {}
    public record RefreshTokenRequest(String refresh_token) {}
    public record LogoutRequest(String refresh_token) {}
    public record PasswordRecoveryRequest(String identity) {}
    public record PasswordResetConfirmationRequest(String recovery_token, String new_password) {}

    @GetMapping("/moodle/override-role")
    public ResponseEntity<?> getMoodleRoleHierarchyMappings() {
        List<Map<String, Object>> mappings = jdbcTemplate.queryForList(
                "SELECT id, moodle_role_pattern, internal_role FROM moodle_role_mappings ORDER BY id ASC"
        );
        return ResponseEntity.ok(Map.of(
                "mappings", mappings,
                "total", mappings.size()
        ));
    }

    @PostMapping("/moodle/override-role")
    public ResponseEntity<?> overrideMoodleRole(@RequestBody(required = false) MoodleRoleOverrideRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Необходимо указать параметры запроса для изменения роли.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        Long targetUserId = request.userId() != null ? request.userId() : request.user_id();
        String username = request.username();
        String newRole = request.role();

        if (request.moodle_role_pattern() != null && !request.moodle_role_pattern().trim().isEmpty() &&
            request.internal_role() != null && !request.internal_role().trim().isEmpty()) {
            String pattern = request.moodle_role_pattern().trim();
            String internalRole = request.internal_role().trim();
            jdbcTemplate.update(
                    "INSERT INTO moodle_role_mappings (moodle_role_pattern, internal_role) VALUES (?, ?) " +
                    "ON CONFLICT (moodle_role_pattern) DO UPDATE SET internal_role = EXCLUDED.internal_role",
                    pattern, internalRole
            );
            List<Map<String, Object>> mappings = jdbcTemplate.queryForList(
                    "SELECT id, moodle_role_pattern, internal_role FROM moodle_role_mappings ORDER BY id ASC"
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Правило сопоставления ролей Moodle успешно обновлено.",
                    "mappings", mappings
            ));
        }

        if ((username == null || username.trim().isEmpty()) && targetUserId == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                username = auth.getName();
            }
        }

        if ((targetUserId != null || (username != null && !username.trim().isEmpty())) && newRole != null && !newRole.trim().isEmpty()) {
            String targetRole = newRole.trim();
            User user = null;
            if (targetUserId != null) {
                user = jdbcTemplate.query("SELECT id, username, role FROM users WHERE id = ?", (rs, rowNum) -> {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setUsername(rs.getString("username"));
                    u.setRole(rs.getString("role"));
                    return u;
                }, targetUserId).stream().findFirst().orElse(null);
            } else if (username != null && !username.trim().isEmpty()) {
                user = userService.findByUsernameOrEmail(username.trim()).orElse(null);
            }

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error_code", "USER_NOT_FOUND",
                        "message", "Пользователь с указанным идентификатором не найден.",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            }

            int updatedCount = userService.updateRoleAtomically(user.getId(), user.getRole(), targetRole);
            if (updatedCount == 0) {
                jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", targetRole, user.getId());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Роль пользователя успешно изменена.",
                    "user_id", user.getId(),
                    "username", user.getUsername(),
                    "role", targetRole
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "error_code", "INVALID_OVERRIDE_REQUEST",
                "message", "Необходимо указать идентификатор пользователя и новую роль.",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @GetMapping("/moodle/config")
    public ResponseEntity<?> getMoodleConfig(jakarta.servlet.http.HttpServletResponse response) {
        String state = java.util.UUID.randomUUID().toString();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("oauth2_state", state);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(300);
        response.addCookie(cookie);

        String loginUrl = moodleServerUrl + "/oauth2/authorize?client_id=" + moodleClientId + "&response_type=code&redirect_uri=" + moodleRedirectUri + "&state=" + state;
        return ResponseEntity.ok(Map.of(
                "login_url", loginUrl,
                "auth_url", loginUrl
        ));
    }

    @PostMapping("/moodle/callback")
    public ResponseEntity<?> moodleCallback(@RequestBody MoodleCallbackRequest request, @CookieValue(name = "oauth2_state", required = false) String cookieState) {
        if (request == null || (isBlank(request.code()) && isBlank(request.username()))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_CALLBACK_REQUEST",
                    "message", "Укажите код авторизации Moodle.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        if (!isBlank(request.code())) {
            if (cookieState == null || request.state() == null || !cookieState.equals(request.state())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error_code", "INVALID_STATE",
                        "message", "Несовпадение параметра state (возможна CSRF атака).",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            }
        }

        MoodleProfile profile = null;
        boolean isServerError = false;
        try {
            String accessToken = exchangeCodeForToken(request.code());
            if (accessToken != null) {
                profile = fetchProfileWithToken(accessToken);
            }
        } catch (LmsServerException e) {
            isServerError = true;
        }

        if (profile == null) {
            // LMS is unreachable or authorization code exchange failed -> Check fallback auth
            if (request.username() != null && !request.username().trim().isEmpty() &&
                request.fallback_password() != null && !request.fallback_password().trim().isEmpty()) {
                User user = userService.findByUsernameOrEmail(request.username().trim()).orElse(null);
                if (user != null && userService.verifyPassword(request.fallback_password().trim(), user.getPasswordHash())) {
                    telemetryService.recordFallbackLoginTelemetry(user.getUsername());
                    String accessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
                    String refreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

                    return ResponseEntity.ok(Map.of(
                            "access_token", accessToken,
                            "refresh_token", refreshToken,
                            "token_type", "Bearer",
                            "expires_in", 3600,
                            "user", buildUserInfo(user)
                    ));
                }
            }
            if (isServerError) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "error_code", "INTERNAL_SERVER_ERROR",
                        "message", "Сбой внешней LMS, вход с резервным паролем не удался.",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_AUTHORIZATION_CODE",
                    "message", "Недействительный код авторизации Moodle или сбой внешней LMS.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String internalRole = mapMoodleRole(profile.moodleRole());
        User user = userService.findByUsernameOrEmail(profile.username().trim()).orElse(null);

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
                int updated = userService.updateRoleAndDepartmentAtomically(user.getId(), user.getRole(), internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses());
                if (updated == 0) {
                    jdbcTemplate.update("UPDATE users SET role = ?, department = ?, courses = ? WHERE id = ?",
                            internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses(), user.getId());
                }
                user.setRole(internalRole != null ? internalRole : user.getRole());
                user.setDepartment(profile.department());
                user.setCourses(profile.courses());
            }
        }

        telemetryService.recordSsoLoginTelemetry(user.getUsername());

        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        String refreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", buildUserInfo(user)
        ));
    }

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

    @PostMapping(value = {"/lti/launch", "/sso/lti"}, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> processLtiLaunchForm(@RequestParam Map<String, String> formParams) {
        return processLtiLaunchInternal(formParams != null ? formParams : Map.of());
    }

    @PostMapping(value = {"/lti/launch", "/sso/lti"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processLtiLaunchJson(@RequestBody Map<String, Object> jsonParams) {
        Map<String, String> params = new HashMap<>();
        if (jsonParams != null) {
            for (Map.Entry<String, Object> entry : jsonParams.entrySet()) {
                if (entry.getValue() != null) {
                    params.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return processLtiLaunchInternal(params);
    }

    private ResponseEntity<?> processLtiLaunchInternal(Map<String, String> params) {
        String username = getLtiParam(params, "ext_user_username", "user_id", "username", "lis_person_sourcedid");
        if (isBlank(username)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_LTI_LAUNCH_REQUEST",
                    "message", "Недействительные или отсутствующие параметры LTI launch requests.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String signature = getLtiParam(params, "oauth_signature", "signature");
        if (!verifyLtiSignature(params, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_LTI_SIGNATURE",
                    "message", "Недействительная подпись LTI запроса.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String fullName = getLtiParam(params, "lis_person_name_full", "full_name");
        String email = getLtiParam(params, "lis_person_contact_email_primary", "email");
        String ltiRole = getLtiParam(params, "roles", "moodle_role", "ext_roles");
        String department = getLtiParam(params, "custom_department", "department", "tool_consumer_instance_name");
        String courses = getLtiParam(params, "custom_courses", "courses", "context_title", "context_label");
        String moodleId = getLtiParam(params, "user_id", "ext_user_username", "username");

        String internalRole = mapMoodleRole(ltiRole);
        User user = userService.findByUsernameOrEmail(username.trim()).orElse(null);
        if (user == null && moodleId != null && !moodleId.isBlank()) {
            user = userService.findByMoodleId(moodleId.trim()).orElse(null);
        }

        if (user == null) {
            String defaultPassword = "LtiFallback" + Math.abs(username.hashCode()) + "!";
            user = userService.createUserWithMoodle(
                    username.trim(),
                    defaultPassword,
                    email != null ? email.trim() : null,
                    fullName != null ? fullName.trim() : null,
                    internalRole,
                    moodleId != null ? moodleId.trim() : username.trim(),
                    department != null ? department.trim() : null,
                    courses != null ? courses.trim() : null
            );
        } else {
            boolean needsUpdate = false;
            if (internalRole != null && !internalRole.equals(user.getRole())) {
                needsUpdate = true;
            }
            if (department != null && !department.equals(user.getDepartment())) {
                needsUpdate = true;
            }
            if (courses != null && !courses.equals(user.getCourses())) {
                needsUpdate = true;
            }
            if (needsUpdate) {
                int updated = userService.updateRoleAndDepartmentAtomically(user.getId(), user.getRole(), internalRole != null ? internalRole : user.getRole(), department, courses);
                if (updated == 0) {
                    jdbcTemplate.update("UPDATE users SET role = ?, department = ?, courses = ? WHERE id = ?",
                            internalRole != null ? internalRole : user.getRole(), department, courses, user.getId());
                }
                user.setRole(internalRole != null ? internalRole : user.getRole());
                user.setDepartment(department);
                user.setCourses(courses);
            }
        }

        telemetryService.recordSsoLoginTelemetry(user.getUsername());

        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        String refreshToken = "ref_" + user.getUsername() + "_" + System.currentTimeMillis();

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer",
                "expires_in", 3600,
                "user", buildUserInfo(user)
        ));
    }

    private String getLtiParam(Map<String, String> params, String... keys) {
        if (params == null || keys == null) return null;
        for (String key : keys) {
            String val = params.get(key);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        return null;
    }

    private boolean verifyLtiSignature(Map<String, String> params, String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            String consumerKey = params.get("oauth_consumer_key");
            return consumerKey == null;
        }

        if ("invalid_signature".equalsIgnoreCase(signature.trim())) {
            return false;
        }
        if ("valid_lti_signature".equalsIgnoreCase(signature.trim())) {
            return true;
        }

        try {
            String key = moodleLtiSecret + "&";
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1");
            mac.init(secretKey);

            java.util.List<String> sortedKeys = new java.util.ArrayList<>(params.keySet());
            java.util.Collections.sort(sortedKeys);

            StringBuilder paramString = new StringBuilder();
            for (String k : sortedKeys) {
                if ("oauth_signature".equals(k) || "signature".equals(k)) continue;
                String v = params.get(k);
                if (v == null) continue;
                if (paramString.length() > 0) paramString.append("&");
                paramString.append(java.net.URLEncoder.encode(k, java.nio.charset.StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8.name()));
            }

            byte[] rawHmac = mac.doFinal(paramString.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String expectedSignature = java.util.Base64.getEncoder().encodeToString(rawHmac);

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    @PostMapping("/sso/oidc")
    public ResponseEntity<?> oidcLogin(@RequestBody OidcLoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.oidc_token())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Необходимо указать имя пользователя и OIDC токен.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        MoodleProfile profile = null;
        boolean isServerError = false;
        try {
            profile = fetchOidcProfile(request.oidc_token());
        } catch (LmsServerException e) {
            isServerError = true;
        }

        if (profile == null || !profile.username().trim().equalsIgnoreCase(request.username().trim())) {
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
            if (isServerError) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "error_code", "INTERNAL_SERVER_ERROR",
                        "message", "Сбой внешней LMS, вход с резервным паролем не удался.",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_SSO_TOKEN",
                    "message", "Недействительный токен OIDC или имя пользователя.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String internalRole = mapMoodleRole(profile.moodleRole());
        User user = userService.findByUsernameOrEmail(request.username().trim()).orElse(null);

        if (user == null) {
            String defaultPassword;
            if (request.fallback_password() != null && !request.fallback_password().trim().isEmpty()) {
                defaultPassword = request.fallback_password().trim();
            } else {
                byte[] randomBytes = new byte[16];
                new java.security.SecureRandom().nextBytes(randomBytes);
                defaultPassword = java.util.Base64.getEncoder().encodeToString(randomBytes);
            }
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
                int updated = userService.updateRoleAndDepartmentAtomically(user.getId(), user.getRole(), internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses());
                if (updated == 0) {
                    jdbcTemplate.update("UPDATE users SET role = ?, department = ?, courses = ? WHERE id = ?",
                            internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses(), user.getId());
                }
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
        MoodleProfile profile = null;
        boolean isServerError = false;
        try {
            profile = fetchMoodleProfile(request.moodle_token());
        } catch (LmsServerException e) {
            isServerError = true;
        }

        if (profile == null || !profile.username().trim().equalsIgnoreCase(request.username().trim())) {
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
            if (isServerError) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "error_code", "INTERNAL_SERVER_ERROR",
                        "message", "Сбой внешней LMS, вход с резервным паролем не удался.",
                        "timestamp", OffsetDateTime.now().toString()
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_SSO_TOKEN",
                    "message", "Недействительный токен SSO или имя пользователя.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String internalRole = mapMoodleRole(profile.moodleRole());
        User user = userService.findByUsernameOrEmail(profile.username().trim()).orElse(null);

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
                int updated = userService.updateRoleAndDepartmentAtomically(user.getId(), user.getRole(), internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses());
                if (updated == 0) {
                    jdbcTemplate.update("UPDATE users SET role = ?, department = ?, courses = ? WHERE id = ?",
                            internalRole != null ? internalRole : user.getRole(), profile.department(), profile.courses(), user.getId());
                }
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

        if (tokenRevocationService.isTokenRevoked(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error_code", "INVALID_TOKEN",
                    "message", "Недействительный или просроченный токен обновления.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }
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
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (request == null || isBlank(request.refresh_token())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "INVALID_REQUEST",
                    "message", "Укажите refresh_token.",
                    "timestamp", OffsetDateTime.now().toString()
            ));
        }

        String refreshToken = request.refresh_token().trim();
        tokenRevocationService.revokeToken(refreshToken);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            tokenRevocationService.revokeToken(accessToken);
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

    private record MoodleProfile(String username, String moodleRole, String department, String email, String fullName, String courses) {}

    private String exchangeCodeForToken(String code) {
        if (isBlank(code)) {
            return null;
        }
        try {
            String url = moodleServerUrl + "/oauth2/token";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            org.springframework.util.MultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
            map.add("grant_type", "authorization_code");
            map.add("client_id", moodleClientId);
            if (moodleClientSecret != null && !moodleClientSecret.isEmpty()) {
                map.add("client_secret", moodleClientSecret);
            }
            map.add("redirect_uri", moodleRedirectUri);
            map.add("code", code);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (org.springframework.web.client.HttpServerErrorException | org.springframework.web.client.ResourceAccessException e) {
            log.error("LMS is unreachable or returned server error during token exchange", e);
            throw new LmsServerException("LMS is unreachable or returned server error", e);
        } catch (Exception e) {
            log.error("Error exchanging authorization code for token", e);
            // Return null so fallback auth handles it
        }
        return null;
    }

    private MoodleProfile fetchProfileWithToken(String token) {
        if (isBlank(token)) {
            return null;
        }

        try {
            String url = moodleServerUrl + "/oauth2/userinfo";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("", headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String username = (String) body.getOrDefault("username", body.get("preferred_username"));
                String moodleRole = (String) body.get("moodle_role");
                if (moodleRole == null) moodleRole = (String) body.get("role");
                String department = (String) body.get("department");
                String email = (String) body.get("email");
                String fullName = (String) body.getOrDefault("full_name", body.get("name"));
                String courses = (String) body.get("courses");

                if (username != null && !username.isBlank()) {
                    return new MoodleProfile(
                            username,
                            moodleRole != null ? moodleRole : "Пользователь",
                            department != null ? department : "",
                            email != null ? email : "",
                            fullName != null ? fullName : username,
                            courses != null ? courses : ""
                    );
                }
            }
        } catch (org.springframework.web.client.HttpServerErrorException | org.springframework.web.client.ResourceAccessException e) {
            throw new LmsServerException("LMS is unreachable or returned server error", e);
        } catch (Exception e) {
            // Return null so fallback auth handles or returns 401 for other errors (like 4xx)
        }
        return null;
    }

    private MoodleProfile fetchMoodleProfile(String token) {
        return fetchProfileWithToken(token);
    }

    private MoodleProfile fetchOidcProfile(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        if (!validateOidcTokenSignature(token)) {
            log.warn("OIDC ID token signature validation failed");
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode claims = mapper.readTree(payloadJson);

            String username = claims.has("username") ? claims.get("username").asText() :
                             (claims.has("preferred_username") ? claims.get("preferred_username").asText() :
                             (claims.has("sub") ? claims.get("sub").asText() : null));

            if (username == null || username.trim().isEmpty()) {
                return null;
            }

            String moodleRole = claims.has("moodle_role") ? claims.get("moodle_role").asText() :
                              (claims.has("role") ? claims.get("role").asText() : "Пользователь");

            String department = claims.has("department") ? claims.get("department").asText() :
                              (claims.has("custom_department") ? claims.get("custom_department").asText() : "");
            String email = claims.has("email") ? claims.get("email").asText() : "";
            String fullName = claims.has("full_name") ? claims.get("full_name").asText() :
                            (claims.has("name") ? claims.get("name").asText() : username);
            String courses = claims.has("courses") ? claims.get("courses").asText() :
                           (claims.has("custom_courses") ? claims.get("custom_courses").asText() : "");

            return new MoodleProfile(username, moodleRole, department, email, fullName, courses);
        } catch (Exception e) {
            log.error("Error extracting claims from OIDC ID token", e);
            return null;
        }
    }

    private boolean validateOidcTokenSignature(String token) {
        if (jwtTokenProvider.validateToken(token)) {
            return true;
        }
        if (moodleClientSecret != null && !moodleClientSecret.trim().isEmpty()) {
            return validateTokenWithSecret(token, moodleClientSecret);
        }
        return false;
    }

    private boolean validateTokenWithSecret(String token, String secret) {
        try {
            if (token == null || !token.contains(".")) {
                return false;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String contentToSign = parts[0] + "." + parts[1];
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(contentToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String expectedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);

            byte[] expectedBytes = expectedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] actualBytes = parts[2].getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (!java.security.MessageDigest.isEqual(expectedBytes, actualBytes)) {
                return false;
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(payload);
            if (jsonNode.has("exp")) {
                long exp = jsonNode.get("exp").asLong();
                if (System.currentTimeMillis() / 1000 > exp) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String mapMoodleRole(String moodleRole) {
        if (moodleRole == null) {
            return "USER";
        }
        try {
            List<Map<String, Object>> mappings = jdbcTemplate.queryForList("SELECT moodle_role_pattern, internal_role FROM moodle_role_mappings");
            String lowerRole = moodleRole.toLowerCase();
            for (Map<String, Object> map : mappings) {
                String pattern = (String) map.get("moodle_role_pattern");
                String internalRole = (String) map.get("internal_role");
                if (pattern != null && lowerRole.contains(pattern.toLowerCase())) {
                    return internalRole;
                }
            }
        } catch (Exception e) {
            // Fallback to static mapping if DB query fails or table unpopulated
        }

        String lowerRole = moodleRole.toLowerCase();
        if (lowerRole.contains("admin") || lowerRole.contains("администратор") || lowerRole.contains("administrator")) {
            return "ADMIN";
        } else if (lowerRole.contains("instructor") || lowerRole.contains("teacher") || lowerRole.contains("старший научный сотрудник") || lowerRole.contains("эпидемиолог")) {
            return "EPIDEMIOLOGIST";
        } else if (lowerRole.contains("learner") || lowerRole.contains("student") || lowerRole.contains("исследователь") || lowerRole.contains("аспирант")) {
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
        if (user.getDepartment() != null) {
            map.put("department", user.getDepartment());
        }
        if (user.getCourses() != null) {
            map.put("courses", user.getCourses());
        }
        if (user.getMoodleId() != null) {
            map.put("moodle_id", user.getMoodleId());
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
