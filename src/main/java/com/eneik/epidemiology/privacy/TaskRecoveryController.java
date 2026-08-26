package com.eneik.epidemiology.privacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery")
public class TaskRecoveryController {

    private final TaskRecoveryService taskRecoveryService;

    @Autowired
    public TaskRecoveryController(TaskRecoveryService taskRecoveryService) {
        this.taskRecoveryService = taskRecoveryService;
    }

    public record ResumeTaskRequest(String action) {}

    @PostMapping("/tasks/{taskId}/resume")
    public ResponseEntity<?> resumeTask(
            @PathVariable UUID taskId,
            @RequestBody(required = false) ResumeTaskRequest request) {

        String action = (request != null && request.action() != null) ? request.action() : "REVIVE_FAILED_TASK";

        RecoveryTask task = taskRecoveryService.resumeTask(taskId, action);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", task.getStatus());
        response.put("message", "Task successfully revived");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(TaskRecoveryService.TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(TaskRecoveryService.TaskNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(TaskRecoveryService.TaskBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(TaskRecoveryService.TaskBadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(TaskRecoveryService.TaskConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(TaskRecoveryService.TaskConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(TaskRecoveryService.TaskRecoveryException.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(TaskRecoveryService.TaskRecoveryException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String errorCode, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error_code", errorCode);
        err.put("message", message);
        err.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(err);
    }
}
