package com.eneik.epidemiology.privacy;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecoveryTaskRepository recoveryTaskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID eligibleTaskId;
    private UUID nonEligibleTaskId;

    @BeforeEach
    void setUp() {
        eligibleTaskId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        RecoveryTask eligibleTask = new RecoveryTask(
                eligibleTaskId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6",
                "FAILED",
                "Task failed due to reconcileClosedUnmergedPullRequest",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        recoveryTaskRepository.save(eligibleTask);

        nonEligibleTaskId = UUID.fromString("8bd0dbae-41f6-466a-95a7-aff680ed0866");
        RecoveryTask nonEligibleTask = new RecoveryTask(
                nonEligibleTaskId,
                "8bd0dbae-41f6-466a-95a7-aff680ed0866",
                "Runtime Contract 9b58412d",
                "FAILED",
                "Unrelated build error",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        recoveryTaskRepository.save(nonEligibleTask);
    }

    @Test
    @DisplayName("Given valid task ID and REVIVE_FAILED_TASK action, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 200 OK and task status IN_PROGRESS returned")
    void testResumeTask_Success() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", eligibleTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.message").value("Task successfully revived"));
    }

    @Test
    @DisplayName("Given invalid operational action, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 400 Bad Request returned")
    void testResumeTask_InvalidAction() throws Exception {
        Map<String, String> body = Map.of("action", "INVALID_ACTION");

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", eligibleTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_ACTION"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given non-eligible task, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 409 Conflict returned")
    void testResumeTask_ConflictIneligible() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", nonEligibleTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("STATE_CONFLICT"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given unknown task UUID, When POST /api/v1/recovery/tasks/{taskId}/resume, Then 404 Not Found returned")
    void testResumeTask_NotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/tasks/{taskId}/resume", unknownId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("TASK_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
