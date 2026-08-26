package com.eneik.epidemiology.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery/tasks")
public class PlannedWorkRecoveryController {

    private static final Logger log = LoggerFactory.getLogger(PlannedWorkRecoveryController.class);
    private final PlannedWorkRecoveryService recoveryService;

    @Autowired
    public PlannedWorkRecoveryController(PlannedWorkRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    public static class ResumeTaskRequest {
        private String action;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    @PostMapping("/{taskId}/resume")
    public ResponseEntity<?> resumeTask(@PathVariable UUID taskId, @RequestBody ResumeTaskRequest request) {
        String actionStr = request.getAction();
        if (actionStr == null || actionStr.isBlank()) {
            log.warn("Validation error for taskId {}: action is required", taskId);
            return ResponseEntity.badRequest().body(Map.of("error", "action is required"));
        }

        OperationalAction action;
        try {
            action = OperationalAction.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error for taskId {}: invalid action '{}'", taskId, actionStr, e);
            return ResponseEntity.badRequest().body(Map.of("error", "invalid action"));
        }

        boolean resumed = recoveryService.resumeTask(taskId, action);
        if (resumed) {
            log.info("Successfully revived taskId {}", taskId);
            return ResponseEntity.ok(Map.of("status", "IN_PROGRESS", "message", "Task successfully revived"));
        } else {
            log.warn("Failed to revive taskId {}: may not exist or not in eligible state", taskId);
            return ResponseEntity.status(409).body(Map.of("error", "Task could not be revived (may not exist or not in eligible state)"));
        }
    }
}
