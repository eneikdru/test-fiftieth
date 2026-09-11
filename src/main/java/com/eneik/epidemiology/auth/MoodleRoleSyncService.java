package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
public class MoodleRoleSyncService {
    private static final Logger log = LoggerFactory.getLogger(MoodleRoleSyncService.class);

    @Value("${moodle.server.url:https://moodle.epidemiology-inst.ru}")
    private String moodleServerUrl = "https://moodle.epidemiology-inst.ru";

    private final UserRepository userRepository;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Autowired
    public MoodleRoleSyncService(UserRepository userRepository, UserService userService, JdbcTemplate jdbcTemplate) {
        this(userRepository, userService, jdbcTemplate, new RestTemplate());
    }

    public MoodleRoleSyncService(UserRepository userRepository, UserService userService, JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    public void setMoodleServerUrl(String moodleServerUrl) {
        this.moodleServerUrl = moodleServerUrl;
    }

    @Scheduled(fixedDelayString = "${moodle.sync.interval-ms:3600000}")
    public Map<String, Object> syncAllUserRoles() {
        log.info("Starting scheduled Moodle role synchronization process");
        List<User> users = userRepository.findAll();
        int totalUsers = 0;
        int syncedUsers = 0;
        int updatedUsers = 0;

        for (User user : users) {
            String moodleId = user.getMoodleId();
            if (moodleId == null || moodleId.trim().isEmpty()) {
                continue;
            }
            totalUsers++;
            try {
                String moodleRole = fetchMoodleUserRole(moodleId);
                if (moodleRole != null) {
                    syncedUsers++;
                    String mappedRole = mapMoodleRole(moodleRole);
                    if (mappedRole != null && !mappedRole.equals(user.getRole())) {
                        int updated = userService.updateRoleAtomically(user.getId(), user.getRole(), mappedRole);
                        if (updated == 0) {
                            jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", mappedRole, user.getId());
                        }
                        user.setRole(mappedRole);
                        updatedUsers++;
                        log.info("Successfully synchronized role for user {}: updated to {}", user.getUsername(), mappedRole);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to synchronize Moodle role for user {}", user.getUsername(), e);
            }
        }

        log.info("Completed Moodle role synchronization. Total Moodle users: {}, Synced: {}, Updated: {}", totalUsers, syncedUsers, updatedUsers);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total_users", totalUsers);
        result.put("synced_users", syncedUsers);
        result.put("updated_users", updatedUsers);
        return result;
    }

    public String fetchMoodleUserRole(String moodleId) {
        if (moodleId == null || moodleId.trim().isEmpty()) {
            return null;
        }
        try {
            String url = moodleServerUrl + "/api/v1/users/" + moodleId.trim() + "/role";
            org.springframework.http.ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String role = (String) body.get("moodle_role");
                if (role == null) role = (String) body.get("role");
                return role;
            }
        } catch (Exception e) {
            log.warn("Direct role query for moodleId {} failed, falling back to profile query if available: {}", moodleId, e.getMessage());
            try {
                String fallbackUrl = moodleServerUrl + "/oauth2/userinfo?username=" + moodleId.trim();
                org.springframework.http.ResponseEntity<Map> response = restTemplate.getForEntity(fallbackUrl, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    String role = (String) body.get("moodle_role");
                    if (role == null) role = (String) body.get("role");
                    return role;
                }
            } catch (Exception ex) {
                log.error("Fallback role query for moodleId {} also failed", moodleId, ex);
            }
        }
        return null;
    }

    public String mapMoodleRole(String moodleRole) {
        if (moodleRole == null || moodleRole.trim().isEmpty()) {
            return "USER";
        }
        try {
            List<Map<String, Object>> mappings = jdbcTemplate.queryForList("SELECT moodle_role_pattern, internal_role FROM moodle_role_mappings ORDER BY id ASC");
            String lowerRole = moodleRole.toLowerCase();
            for (Map<String, Object> map : mappings) {
                String pattern = (String) map.get("moodle_role_pattern");
                String internalRole = (String) map.get("internal_role");
                if (pattern != null && lowerRole.contains(pattern.toLowerCase())) {
                    return internalRole;
                }
            }
        } catch (Exception e) {
            log.warn("Database role mapping query failed, using static fallback rules", e);
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
}
