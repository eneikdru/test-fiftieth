package com.eneik.epidemiology.privacy;

import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.security.JwtAuthenticationFilter;
import com.eneik.epidemiology.security.JwtTokenProvider;
import com.eneik.epidemiology.security.SecurityConfig;
import com.eneik.epidemiology.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskRecoveryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TaskRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskRecoveryService taskRecoveryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserService userService;

    private UUID eligibleTaskId;
    private UUID nonEligibleTaskId;
    private String validToken;

    @BeforeEach
    void setUp() {
        eligibleTaskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        nonEligibleTaskId = UUID.fromString("8bd0dbae-41f6-466a-95a7-aff680ed0866");

        validToken = "mock_valid_recovery_user_token";
        Mockito.when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        Mockito.when(tokenRevocationService.isTokenRevoked(validToken)).thenReturn(false);
        Mockito.when(jwtTokenProvider.getUsername(validToken)).thenReturn("admin_user");
        Mockito.when(jwtTokenProvider.getRole(validToken)).thenReturn("ADMIN");
    }

    @Test
    @DisplayName("Given an unauthenticated request to /api/v1/recovery/tasks/{taskId}/resume, When received, Then it is strictly rejected with 401 Unauthorized")
    void testResumeTask_Unauthenticated_Rejected401() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", eligibleTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code", is("UNAUTHORIZED")))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Given valid token, task ID, and REVIVE_FAILED_TASK action, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 200 OK and task status IN_PROGRESS returned")
    void testResumeTask_Success() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        RecoveryTask task = new RecoveryTask(
                eligibleTaskId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6",
                "IN_PROGRESS",
                "Task failed due to reconcileClosedUnmergedPullRequest",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        Mockito.when(taskRecoveryService.resumeTask(eq(eligibleTaskId), eq("REVIVE_FAILED_TASK")))
                .thenReturn(task);

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", eligibleTaskId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.message").value("Task successfully revived"));
    }

    @Test
    @DisplayName("Given valid token but invalid operational action, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 400 Bad Request returned")
    void testResumeTask_InvalidAction() throws Exception {
        Map<String, String> body = Map.of("action", "INVALID_ACTION");

        Mockito.when(taskRecoveryService.resumeTask(eq(eligibleTaskId), eq("INVALID_ACTION")))
                .thenThrow(new TaskRecoveryService.TaskBadRequestException("INVALID_ACTION", "Operational action INVALID_ACTION is not recognized"));

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", eligibleTaskId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_ACTION"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given valid token but non-eligible task, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 409 Conflict returned")
    void testResumeTask_ConflictIneligible() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        Mockito.when(taskRecoveryService.resumeTask(eq(nonEligibleTaskId), eq("REVIVE_FAILED_TASK")))
                .thenThrow(new TaskRecoveryService.TaskConflictException("STATE_CONFLICT", "Task is not eligible for recovery"));

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", nonEligibleTaskId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("STATE_CONFLICT"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given valid token but unknown task UUID, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 404 Not Found returned")
    void testResumeTask_NotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        Mockito.when(taskRecoveryService.resumeTask(eq(unknownId), eq("REVIVE_FAILED_TASK")))
                .thenThrow(new TaskRecoveryService.TaskNotFoundException("TASK_NOT_FOUND", "Task not found with ID: " + unknownId));

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", unknownId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("TASK_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
