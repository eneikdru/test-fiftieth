package com.eneik.epidemiology.privacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private final PrivacyService privacyService;

    @Autowired
    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @PostMapping("/export-requests")
    public ResponseEntity<?> createDataExportRequest(@RequestBody Map<String, Object> body) {
        String subjectId = (String) body.get("subject_id");
        String requestedFormat = (String) body.get("requested_format");
        String notes = (String) body.get("notes");

        DataExportJob job = privacyService.initiateDataExport(subjectId, requestedFormat, notes);
        Map<String, Object> response = mapExportJobResponse(job);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/export-requests/{requestId}")
    public ResponseEntity<?> getDataExportJobStatus(@PathVariable String requestId) {
        DataExportJob job = privacyService.getExportJobStatus(requestId);
        Map<String, Object> response = mapExportJobResponse(job);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-requests/{requestId}/download")
    public ResponseEntity<byte[]> downloadExportedData(@PathVariable String requestId) {
        PrivacyService.DownloadData downloadData = privacyService.getExportDownloadData(requestId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(downloadData.mediaType()));
        return new ResponseEntity<>(downloadData.bytes(), headers, HttpStatus.OK);
    }

    @PostMapping("/erasure-requests")
    public ResponseEntity<?> createDataErasureRequest(@RequestBody Map<String, Object> body) {
        String subjectId = (String) body.get("subject_id");
        String confirmationToken = (String) body.get("confirmation_token");
        String reason = (String) body.get("reason");
        String erasureScope = (String) body.get("erasure_scope");

        DataErasureJob job = privacyService.initiateDataErasure(subjectId, confirmationToken, reason, erasureScope);
        Map<String, Object> response = mapErasureJobResponse(job);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/erasure-requests/{requestId}")
    public ResponseEntity<?> getDataErasureJobStatus(@PathVariable String requestId) {
        DataErasureJob job = privacyService.getErasureJobStatus(requestId);
        Map<String, Object> response = mapErasureJobResponse(job);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapExportJobResponse(DataExportJob job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("request_id", job.getRequestId());
        map.put("subject_id", job.getSubjectId());
        map.put("status", job.getStatus());
        map.put("download_url", job.getDownloadUrl());
        map.put("created_at", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        map.put("completed_at", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        map.put("expires_at", job.getExpiresAt() != null ? job.getExpiresAt().toString() : null);
        if (job.getErrorCode() != null) {
            Map<String, String> errDetails = new LinkedHashMap<>();
            errDetails.put("code", job.getErrorCode());
            errDetails.put("message", job.getErrorMessage());
            map.put("error_details", errDetails);
        } else {
            map.put("error_details", null);
        }
        return map;
    }

    private Map<String, Object> mapErasureJobResponse(DataErasureJob job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("request_id", job.getRequestId());
        map.put("subject_id", job.getSubjectId());
        map.put("status", job.getStatus());
        map.put("created_at", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        map.put("completed_at", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        map.put("records_erased_count", job.getRecordsErasedCount());
        if (job.getErrorCode() != null) {
            Map<String, String> errDetails = new LinkedHashMap<>();
            errDetails.put("code", job.getErrorCode());
            errDetails.put("message", job.getErrorMessage());
            map.put("error_details", errDetails);
        } else {
            map.put("error_details", null);
        }
        return map;
    }

    @ExceptionHandler(PrivacyService.PrivacyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PrivacyService.PrivacyNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(PrivacyService.PrivacyBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(PrivacyService.PrivacyBadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(PrivacyService.PrivacyConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(PrivacyService.PrivacyConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(PrivacyService.PrivacyGoneException.class)
    public ResponseEntity<Map<String, Object>> handleGone(PrivacyService.PrivacyGoneException ex) {
        return buildErrorResponse(HttpStatus.GONE, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(PrivacyService.PrivacyException.class)
    public ResponseEntity<Map<String, Object>> handleGenericPrivacyException(PrivacyService.PrivacyException ex) {
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
