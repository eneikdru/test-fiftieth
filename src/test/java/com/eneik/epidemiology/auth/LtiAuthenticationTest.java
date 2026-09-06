package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LtiAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Given valid LTI launch form request, When POST /api/v1/auth/lti/launch called, Then authenticates, provisions user, syncs role/department and returns JWT tokens")
    void testLtiLaunch_ValidFormRequest_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("lti_message_type", "basic-lti-launch-request")
                .param("lti_version", "LTI-1p0")
                .param("resource_link_id", "res_998877")
                .param("user_id", "moodle_lti_user_101")
                .param("ext_user_username", "lti_researcher_john")
                .param("lis_person_name_full", "John Doe")
                .param("lis_person_contact_email_primary", "john.doe@moodle.org")
                .param("roles", "Instructor,Researcher")
                .param("tool_consumer_instance_name", "Department of Virology")
                .param("context_title", "VIR-501")
                .param("oauth_consumer_key", "moodle_key_valid")
                .param("oauth_signature", "valid_signature_hash_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", startsWith("ref_lti_researcher_john_")))
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("lti_researcher_john")))
                .andExpect(jsonPath("$.user.role", is("EPIDEMIOLOGIST")));

        Optional<User> userOpt = userService.findByMoodleId("moodle_lti_user_101");
        assertTrue(userOpt.isPresent(), "User should be auto-provisioned upon successful LTI launch");
        User user = userOpt.get();
        assertEquals("lti_researcher_john", user.getUsername());
        assertEquals("Department of Virology", user.getDepartment());
        assertEquals("VIR-501", user.getCourses());
        assertEquals("EPIDEMIOLOGIST", user.getRole());
    }

    @Test
    @DisplayName("Given existing user and updated LTI roles/department, When LTI launch called, Then updates user role and department atomically")
    void testLtiLaunch_ExistingUser_AtomicUpdateSuccess() throws Exception {
        userService.createUserWithMoodle("lti_existing_user", "Pass123!", "exist@inst.ru", "Existing User", "USER", "moodle_lti_existing_99", "Old Dept", "OLD-101");

        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("lti_message_type", "basic-lti-launch-request")
                .param("lti_version", "LTI-1p0")
                .param("resource_link_id", "res_112233")
                .param("user_id", "moodle_lti_existing_99")
                .param("ext_user_username", "lti_existing_user")
                .param("roles", "Administrator")
                .param("tool_consumer_instance_name", "Department of Molecular Biology")
                .param("context_title", "MOL-202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("ADMIN")));

        Optional<User> userOpt = userService.findByMoodleId("moodle_lti_existing_99");
        assertTrue(userOpt.isPresent());
        User updatedUser = userOpt.get();
        assertEquals("ADMIN", updatedUser.getRole());
        assertEquals("Department of Molecular Biology", updatedUser.getDepartment());
        assertEquals("MOL-202", updatedUser.getCourses());
    }

    @Test
    @DisplayName("Given LTI launch request with invalid signature, When processed, Then returns 401 UNAUTHORIZED with Russian error message")
    void testLtiLaunch_InvalidSignature_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("lti_message_type", "basic-lti-launch-request")
                .param("lti_version", "LTI-1p0")
                .param("resource_link_id", "res_123")
                .param("user_id", "moodle_user_1")
                .param("oauth_signature", "invalid_signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code", is("INVALID_LTI_SIGNATURE")))
                .andExpect(jsonPath("$.message", containsString("Недействительная подпись LTI запроса")));
    }

    @Test
    @DisplayName("Given LTI launch request missing mandatory resource_link_id, When processed, Then returns 400 BAD_REQUEST")
    void testLtiLaunch_MissingResourceLinkId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/lti/launch")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("lti_message_type", "basic-lti-launch-request")
                .param("lti_version", "LTI-1p0")
                .param("user_id", "moodle_user_2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_LTI_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("resource_link_id")));
    }
}
