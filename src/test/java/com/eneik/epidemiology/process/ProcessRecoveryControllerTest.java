package com.eneik.epidemiology.process;

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProcessRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BackgroundProcessRepository recoveryProcessRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID eligibleProcessId;
    private UUID nonEligibleProcessId;

    @BeforeEach
    void setUp() {
        eligibleProcessId = UUID.fromString("5421d1f0-ec82-43a9-ad0c-9a94345450af");
        BackgroundProcess eligibleProcess = new BackgroundProcess(
                eligibleProcessId,
                "5421d1f0-ec82-43a9-ad0c-9a94345450af",
                "API Slice D3a7a0f6",
                "FAILED",
                "Process failed due to data_processing_error",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        recoveryProcessRepository.save(eligibleProcess);

        nonEligibleProcessId = UUID.fromString("8bd0dbae-41f6-466a-95a7-aff680ed0866");
        BackgroundProcess nonEligibleProcess = new BackgroundProcess(
                nonEligibleProcessId,
                "8bd0dbae-41f6-466a-95a7-aff680ed0866",
                "Runtime Contract 9b58412d",
                "FAILED",
                "Unrelated build error",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        recoveryProcessRepository.save(nonEligibleProcess);
    }

    @Test
    @DisplayName("Given valid process ID and REVIVE_FAILED_TASK action, When POST /api/v1/recovery/processes/{processId}/resume, Then 200 OK and process status IN_PROGRESS returned")
    void testResumeProcess_Success() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/processes/{processId}/resume", eligibleProcessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.message").value("Process successfully revived"));
    }

    @Test
    @DisplayName("Given invalid operational action, When POST /api/v1/recovery/processes/{processId}/resume, Then 400 Bad Request returned")
    void testResumeProcess_InvalidAction() throws Exception {
        Map<String, String> body = Map.of("action", "INVALID_ACTION");

        mockMvc.perform(post("/api/v1/recovery/processes/{processId}/resume", eligibleProcessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_ACTION"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given non-eligible process, When POST /api/v1/recovery/processes/{processId}/resume, Then 409 Conflict returned")
    void testResumeProcess_ConflictIneligible() throws Exception {
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/processes/{processId}/resume", nonEligibleProcessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("STATE_CONFLICT"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Given unknown process UUID, When POST /api/v1/recovery/processes/{processId}/resume, Then 404 Not Found returned")
    void testResumeProcess_NotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        Map<String, String> body = Map.of("action", "REVIVE_FAILED_TASK");

        mockMvc.perform(post("/api/v1/recovery/processes/{processId}/resume", unknownId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("PROCESS_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
