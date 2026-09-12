package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class CourseBasedAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthController authController;

    private org.springframework.test.web.client.MockRestServiceServer mockServer;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockServer = org.springframework.test.web.client.MockRestServiceServer.createServer(authController.getRestTemplate());
    }

    @Test
    @DisplayName("Given valid OIDC payload, When logging in, Then course based access is synced")
    void testCourseAccessSync() throws Exception {
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer mock_valid_new_moodle_token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"username\":\"course_user\",\"moodle_role\":\"Пользователь\",\"department\":\"IT\",\"email\":\"course@inst.ru\",\"full_name\":\"Course User\",\"courses\":\"CS101\"}",
                        MediaType.APPLICATION_JSON));

        userService.createUser("course_user", "Pass123!", "course@inst.ru", "Course User", "USER");

        String ssoBody = "{\"username\":\"course_user\",\"moodle_token\":\"mock_valid_new_moodle_token\"}";

        mockMvc.perform(post("/api/v1/auth/sso/moodle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoBody))
                .andExpect(status().isOk());

        User user = userRepository.findByUsername("course_user").orElseThrow();
        assert "CS101".equals(user.getCourses());
    }
}
