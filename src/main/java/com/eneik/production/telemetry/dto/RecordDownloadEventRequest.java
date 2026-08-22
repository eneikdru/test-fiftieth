package com.eneik.production.telemetry.dto;

import jakarta.validation.constraints.NotBlank;

public class RecordDownloadEventRequest {

    @NotBlank(message = "Document ID is required")
    private String documentId;

    private String userId;

    public RecordDownloadEventRequest() {
    }

    public RecordDownloadEventRequest(String documentId, String userId) {
        this.documentId = documentId;
        this.userId = userId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
