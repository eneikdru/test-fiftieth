package com.eneik.epidemiology.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.eneik.epidemiology.security.SecurityConfig;
import com.eneik.epidemiology.security.JwtTokenProvider;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.mock.mockito.MockBean;

@WebMvcTest(controllers = PlannedWorkRecoveryController.class)
@Import({SecurityConfig.class})
public class PlannedWorkRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    @DisplayName("Given a valid request, when resuming a task, it should return success")
    public void resumeTask_success() throws Exception {
        UUID taskId = UUID.randomUUID();
        Map<String, String> request = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/tasks/" + taskId + "/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("Task successfully revived"));
    }

    @Test
    @WithMockUser
    @DisplayName("Given an invalid action, when resuming a task, it should return bad request")
    public void resumeTask_invalidAction() throws Exception {
        UUID taskId = UUID.randomUUID();
        Map<String, String> request = Map.of("action", "INVALID_ACTION");

        mockMvc.perform(post("/api/v1/recovery/tasks/" + taskId + "/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
