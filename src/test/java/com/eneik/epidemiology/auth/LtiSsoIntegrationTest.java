package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class LtiSsoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @BeforeEach
    void setUp() {
        telemetryEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Given valid LTI launch request from Moodle with valid signature, When POST /api/v1/auth/lti/launch received, Then user is authenticated, synced with role and department, and token returned")
    void testLtiLaunch_ValidParametersAndSignature_AuthenticatesAndSyncs() throws Exception {
        java.util.Map<String, String> params = new java.util.TreeMap<>();
        params.put("user_id", "moodle_lti_100");
        params.put("ext_user_username", "lti_epidemiologist");
        params.put("lis_person_name_full", "Сергеев Сергей Сергеевич");
        params.put("lis_person_contact_email_primary", "sergeev@epidemiology-inst.ru");
        params.put("roles", "Instructor");
        params.put("custom_department", "Кафедра Вирусологии");
        params.put("custom_courses", "VIR-101,VIR-202");
        params.put("oauth_consumer_key", "moodle_lti_key");

        String computedSignature = calculateLtiHmacSha1(params, "moodle_lti_secret");

        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .param("user_id", params.get("user_id"))
                .param("ext_user_username", params.get("ext_user_username"))
                .param("lis_person_name_full", params.get("lis_person_name_full"))
                .param("lis_person_contact_email_primary", params.get("lis_person_contact_email_primary"))
                .param("roles", params.get("roles"))
                .param("custom_department", params.get("custom_department"))
                .param("custom_courses", params.get("custom_courses"))
                .param("oauth_consumer_key", params.get("oauth_consumer_key"))
                .param("oauth_signature", computedSignature)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("lti_epidemiologist")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")))
                .andExpect(jsonPath("$.user.department", is("Кафедра Вирусологии")))
                .andExpect(jsonPath("$.user.courses", is("VIR-101,VIR-202")))
                .andExpect(jsonPath("$.user.email", is("sergeev@epidemiology-inst.ru")));

        User user = userRepository.findByUsername("lti_epidemiologist").orElseThrow();
        assert "EPIDEMIOLOGIST".equals(user.getRole());
        assert "Кафедра Вирусологии".equals(user.getDepartment());
        assert "VIR-101,VIR-202".equals(user.getCourses());

        long ssoEvents = telemetryEventRepository.findAll().stream()
                .filter(e -> "sso_login_success".equals(e.getEventType()) && "lti_epidemiologist".equals(e.getQueryTerm()))
                .count();
        assert ssoEvents == 1;
    }

    @Test
    @DisplayName("Given valid LTI launch request with actual HMAC-SHA1 calculated signature, When POST /api/v1/auth/lti/launch received, Then signature is validated and user authenticated")
    void testLtiLaunch_HmacSha1CalculatedSignature_Success() throws Exception {
        java.util.Map<String, String> params = new java.util.TreeMap<>();
        params.put("custom_department", "Эпидемиология");
        params.put("ext_user_username", "hmac_lti_user");
        params.put("lis_person_contact_email_primary", "hmac@epidemiology-inst.ru");
        params.put("lis_person_name_full", "Иван Иваночев");
        params.put("roles", "Instructor");
        params.put("user_id", "moodle_lti_200");

        String secret = "moodle_lti_secret&";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKey);

        StringBuilder paramString = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            if (paramString.length() > 0) paramString.append("&");
            paramString.append(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8.name()))
                    .append("=")
                    .append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8.name()));
        }

        byte[] rawHmac = mac.doFinal(paramString.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String computedSignature = java.util.Base64.getEncoder().encodeToString(rawHmac);

        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .param("user_id", params.get("user_id"))
                .param("ext_user_username", params.get("ext_user_username"))
                .param("lis_person_name_full", params.get("lis_person_name_full"))
                .param("lis_person_contact_email_primary", params.get("lis_person_contact_email_primary"))
                .param("roles", params.get("roles"))
                .param("custom_department", params.get("custom_department"))
                .param("oauth_signature", computedSignature)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("hmac_lti_user")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));
    }

    @Test
    @DisplayName("Given LTI launch request with invalid OAuth signature, When POST /api/v1/auth/lti/launch called, Then returns 401 Unauthorized")
    void testLtiLaunch_InvalidSignature_Returns401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .param("ext_user_username", "lti_user_bad_sig")
                .param("roles", "Instructor")
                .param("oauth_consumer_key", "moodle_lti_key")
                .param("oauth_signature", "invalid_signature")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_LTI_SIGNATURE")))
                .andExpect(jsonPath("$.message", is("Недействительная подпись LTI запроса.")));
    }

    @Test
    @DisplayName("Given LTI launch request missing mandatory user parameters, When POST /api/v1/auth/lti/launch called, Then returns 400 Bad Request")
    void testLtiLaunch_MissingParameters_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .param("custom_department", "Эпидемиология")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_LTI_LAUNCH_REQUEST")))
                .andExpect(jsonPath("$.message", is("Недействительные или отсутствующие параметры LTI launch requests.")));
    }

    @Test
    @DisplayName("Given existing user, When LTI launch request arrives with updated role and department, Then user profile is updated atomically")
    void testLtiLaunch_ExistingUserRoleAndDepartmentUpdate() throws Exception {
        userService.createUser("lti_existing_user", "OldPass123!", "existing@inst.ru", "Существующий Пользователь", "USER");

        java.util.Map<String, String> params = new java.util.TreeMap<>();
        params.put("username", "lti_existing_user");
        params.put("roles", "Administrator");
        params.put("custom_department", "Департамент Аналитики");

        String computedSignature = calculateLtiHmacSha1(params, "moodle_lti_secret");

        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .param("username", params.get("username"))
                .param("roles", params.get("roles"))
                .param("custom_department", params.get("custom_department"))
                .param("oauth_signature", computedSignature)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("lti_existing_user")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")))
                .andExpect(jsonPath("$.user.department", is("Департамент Аналитики")));

        User updatedUser = userRepository.findByUsername("lti_existing_user").orElseThrow();
        assert "ADMIN".equals(updatedUser.getRole());
        assert "Департамент Аналитики".equals(updatedUser.getDepartment());
    }

    private String calculateLtiHmacSha1(java.util.Map<String, String> params, String ltiSecret) throws Exception {
        String secret = ltiSecret + "&";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKey);

        java.util.List<String> sortedKeys = new java.util.ArrayList<>(params.keySet());
        java.util.Collections.sort(sortedKeys);

        StringBuilder paramString = new StringBuilder();
        for (String k : sortedKeys) {
            String v = params.get(k);
            if (v == null) continue;
            if (paramString.length() > 0) paramString.append("&");
            paramString.append(java.net.URLEncoder.encode(k, java.nio.charset.StandardCharsets.UTF_8.name()))
                    .append("=")
                    .append(java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8.name()));
        }

        byte[] rawHmac = mac.doFinal(paramString.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(rawHmac);
    }
}
