package com.eneik.epidemiology.recovery;

import com.eneik.epidemiology.EpidemiologyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EpidemiologyApplication.class)
@AutoConfigureMockMvc
class PlannedWorkRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlannedWorkRecoveryService plannedWorkRecoveryService;

    @Test
    @DisplayName("Given a failed task, When calling resume endpoint, Then it resumes successfully")
    @WithMockUser(roles = "ADMIN")
    void testResumeTaskEndpoint() throws Exception {
        UUID taskId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        when(plannedWorkRecoveryService.resumeTask(eq(taskId), eq(OperationalAction.REVIVE_FAILED_TASK)))
                .thenReturn(true);

        String payload = "{\"action\": \"REVIVE_FAILED_TASK\"}";

        mockMvc.perform(post("/api/v1/recovery/tasks/" + taskId.toString() + "/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Given an invalid action, When calling resume endpoint, Then it returns a bad request")
    @WithMockUser(roles = "ADMIN")
    void testResumeTaskEndpointInvalidAction() throws Exception {
        UUID taskId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        String payload = "{\"action\": \"INVALID_ACTION\"}";

        mockMvc.perform(post("/api/v1/recovery/tasks/" + taskId.toString() + "/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_ACTION"));
    }

    @Test
    @DisplayName("Given an ineligible task, When calling resume endpoint, Then it returns a conflict status")
    @WithMockUser(roles = "ADMIN")
    void testResumeTaskEndpointConflict() throws Exception {
        UUID taskId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        when(plannedWorkRecoveryService.resumeTask(eq(taskId), eq(OperationalAction.REVIVE_FAILED_TASK)))
                .thenReturn(false);

        String payload = "{\"action\": \"REVIVE_FAILED_TASK\"}";

        mockMvc.perform(post("/api/v1/recovery/tasks/" + taskId.toString() + "/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("CONFLICT"));
    }
}
