package com.eneik.epidemiology.controller;

import com.eneik.epidemiology.dto.DataErasureJobResponse;
import com.eneik.epidemiology.dto.DataErasureRequest;
import com.eneik.epidemiology.dto.DataExportJobResponse;
import com.eneik.epidemiology.dto.DataExportRequest;
import com.eneik.epidemiology.service.PrivacyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @PostMapping("/export-requests")
    public ResponseEntity<DataExportJobResponse> createExportRequest(@Valid @RequestBody DataExportRequest request) {
        DataExportJobResponse response = privacyService.createExportRequest(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/export-requests/{requestId}")
    public ResponseEntity<DataExportJobResponse> getExportJobStatus(@PathVariable UUID requestId) {
        DataExportJobResponse response = privacyService.getExportJobStatus(requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-requests/{requestId}/download")
    public ResponseEntity<byte[]> downloadExportedData(@PathVariable UUID requestId) {
        byte[] content = privacyService.downloadExportPackage(requestId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "personal_data_" + requestId + ".zip");
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @PostMapping("/erasure-requests")
    public ResponseEntity<DataErasureJobResponse> createErasureRequest(@Valid @RequestBody DataErasureRequest request) {
        DataErasureJobResponse response = privacyService.createErasureRequest(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/erasure-requests/{requestId}")
    public ResponseEntity<DataErasureJobResponse> getErasureJobStatus(@PathVariable UUID requestId) {
        DataErasureJobResponse response = privacyService.getErasureJobStatus(requestId);
        return ResponseEntity.ok(response);
    }
}
