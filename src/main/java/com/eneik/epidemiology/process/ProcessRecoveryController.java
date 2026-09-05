package com.eneik.epidemiology.process;

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
public class ProcessRecoveryController {

    private final ProcessRecoveryService processRecoveryService;
    private final Clock clock;

    @Autowired
    public ProcessRecoveryController(ProcessRecoveryService processRecoveryService) {
        this(processRecoveryService, Clock.systemUTC());
    }

    public ProcessRecoveryController(ProcessRecoveryService processRecoveryService, Clock clock) {
        this.processRecoveryService = processRecoveryService;
        this.clock = clock;
    }

    public record ResumeProcessRequest(String action) {}

    @PostMapping("/processes/{processId}/resume")
    public ResponseEntity<?> resumeProcess(
            @PathVariable UUID processId,
            @RequestBody(required = false) ResumeProcessRequest request) {

        String action = (request != null && request.action() != null) ? request.action() : "REVIVE_FAILED_TASK";

        BackgroundProcess process = processRecoveryService.resumeProcess(processId, action);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", process.getStatus());
        response.put("message", "Process successfully revived");
        return ResponseEntity.ok(response);
    }

    public record ErrorResponse(String error_code, String message, String timestamp) {}

    @ExceptionHandler(ProcessRecoveryService.ProcessNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProcessRecoveryService.ProcessNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(ProcessRecoveryService.ProcessBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(ProcessRecoveryService.ProcessBadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(ProcessRecoveryService.ProcessConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ProcessRecoveryService.ProcessConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(ProcessRecoveryService.ProcessRecoveryException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(ProcessRecoveryService.ProcessRecoveryException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorCode, String message) {
        ErrorResponse err = new ErrorResponse(errorCode, message, OffsetDateTime.now(clock).toString());
        return ResponseEntity.status(status).body(err);
    }
}
