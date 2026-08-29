package com.eneik.epidemiology.privacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery")
public class TaskRecoveryController {

    private final TaskRecoveryService taskRecoveryService;
    private final Clock clock;

    @Autowired
    public TaskRecoveryController(TaskRecoveryService taskRecoveryService) {
        this(taskRecoveryService, Clock.systemUTC());
    }

    public TaskRecoveryController(TaskRecoveryService taskRecoveryService, Clock clock) {
        this.taskRecoveryService = taskRecoveryService;
        this.clock = clock;
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

    public record ErrorResponse(String error_code, String message, String timestamp) {}

    @ExceptionHandler(TaskRecoveryService.TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TaskRecoveryService.TaskNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(TaskRecoveryService.TaskBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(TaskRecoveryService.TaskBadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(TaskRecoveryService.TaskConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(TaskRecoveryService.TaskConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(TaskRecoveryService.TaskRecoveryException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(TaskRecoveryService.TaskRecoveryException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorCode, String message) {
        ErrorResponse err = new ErrorResponse(errorCode, message, OffsetDateTime.now(clock).toString());
        return ResponseEntity.status(status).body(err);
    }
}
