package com.eneik.epidemiology.privacy;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "privacy_export_requests")
public class DataExportJob {

    @Id
    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "subject_id", length = 100, nullable = false)
    private String subjectId;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "requested_format", length = 20, nullable = false)
    private String requestedFormat;

    @Column(name = "download_url", length = 255)
    private String downloadUrl;

    @Column(name = "notes", length = 500)
    private String notes;

    @Lob
    @Column(name = "export_payload")
    private String exportPayload;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public DataExportJob() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestedFormat() {
        return requestedFormat;
    }

    public void setRequestedFormat(String requestedFormat) {
        this.requestedFormat = requestedFormat;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExportPayload() {
        return exportPayload;
    }

    public void setExportPayload(String exportPayload) {
        this.exportPayload = exportPayload;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
