package com.eneik.epidemiology.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
        @NotNull(message = "action is required")
        private OperationalAction action;

        public OperationalAction getAction() {
            return action;
        }

        public void setAction(OperationalAction action) {
            this.action = action;
        }
    }

    @PostMapping("/{taskId}/resume")
    public ResponseEntity<?> resumeTask(@PathVariable UUID taskId, @Valid @RequestBody ResumeTaskRequest request) {
        OperationalAction action = request.getAction();

        boolean resumed = recoveryService.resumeTask(taskId, action);
        if (resumed) {
            log.info("Successfully revived taskId {}", taskId);
            return ResponseEntity.ok(Map.of("status", "IN_PROGRESS", "message", "Task successfully revived"));
        } else {
            log.warn("Failed to revive taskId {}: may not exist or not in eligible state", taskId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error_code", "CONFLICT", "message", "Task could not be revived (may not exist or not in eligible state)"));
        }
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("Malformed request received: {}", ex.getMessage());
        if (ex.getMessage() != null && ex.getMessage().contains("OperationalAction")) {
            return ResponseEntity.badRequest().body(Map.of("error_code", "INVALID_ACTION", "message", "invalid action"));
        }
        return ResponseEntity.badRequest().body(Map.of("error_code", "VALIDATION_ERROR", "message", "malformed request"));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error_code", "VALIDATION_ERROR", "message", "action is required"));
    }
}
