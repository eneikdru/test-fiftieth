package com.eneik.epidemiology.auth;

import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import com.eneik.epidemiology.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@AutoConfigureMockMvc
@io.zonky.test.db.AutoConfigureEmbeddedDatabase
@Transactional
public class MoodleSyncConcurrencyAndIntegrationTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private UserService userService;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(authController.getRestTemplate());
    }

    @Test
    @DisplayName("Given Moodle user, When performMoodleRoleSync executes, Then queries external Moodle endpoint and updates role, department, and courses")
    void testPerformMoodleRoleSyncQueriesExternalEndpoint() {
        User user = userService.createUserWithMoodle(
                "external_sync_user",
                "Pass123!",
                "extsync@inst.ru",
                "External Sync User",
                "USER",
                "mock_moodle_token_123",
                "Old Dept",
                "EPID-101"
        );

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer mock_moodle_token_123"))
                .andRespond(withSuccess(
                        "{\"username\":\"external_sync_user\",\"moodle_role\":\"Администратор\",\"department\":\"Epidemiology Dept\",\"email\":\"extsync@inst.ru\",\"full_name\":\"External Sync User\",\"courses\":\"EPID-201,EPID-301\"}",
                        MediaType.APPLICATION_JSON
                ));

        authController.syncMoodleRoles();

        mockServer.verify();

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assert "ADMIN".equals(updatedUser.getRole());
        assert "Epidemiology Dept".equals(updatedUser.getDepartment());
        assert "EPID-201,EPID-301".equals(updatedUser.getCourses());
    }

    @Test
    @DisplayName("Given atomic update returns 0 on concurrent modification, When performMoodleRoleSync executes, Then OptimisticLockingFailureException is thrown")
    void testPerformMoodleRoleSyncThrowsOptimisticLockingFailureException() {
        User user = userService.createUserWithMoodle(
                "concurrent_user",
                "Pass123!",
                "concurrent@inst.ru",
                "Concurrent User",
                "USER",
                "mock_moodle_token_concurrent",
                "IT",
                "EPID-202"
        );

        mockServer.expect(requestTo("https://moodle.epidemiology-inst.ru/oauth2/userinfo"))
                .andExpect(header("Authorization", "Bearer mock_moodle_token_concurrent"))
                .andRespond(withSuccess(
                        "{\"username\":\"concurrent_user\",\"moodle_role\":\"Администратор\",\"department\":\"Updated Dept\",\"email\":\"concurrent@inst.ru\",\"full_name\":\"Concurrent User\",\"courses\":\"EPID-202\"}",
                        MediaType.APPLICATION_JSON
                ));

        given(userService.updateRoleAndDepartmentAtomically(anyLong(), anyString(), anyString(), anyString(), anyString())).willReturn(0);

        assertThrows(OptimisticLockingFailureException.class, () -> {
            authController.syncMoodleRoles();
        });
    }
}
