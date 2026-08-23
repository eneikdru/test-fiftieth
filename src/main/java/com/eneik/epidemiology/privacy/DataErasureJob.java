package com.eneik.epidemiology.privacy;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "privacy_erasure_requests")
public class DataErasureJob {

    @Id
    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "subject_id", length = 100, nullable = false)
    private String subjectId;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "confirmation_token", length = 255, nullable = false)
    private String confirmationToken;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "erasure_scope", length = 50, nullable = false)
    private String erasureScope;

    @Column(name = "records_erased_count")
    private Integer recordsErasedCount;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public DataErasureJob() {
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

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getErasureScope() {
        return erasureScope;
    }

    public void setErasureScope(String erasureScope) {
        this.erasureScope = erasureScope;
    }

    public Integer getRecordsErasedCount() {
        return recordsErasedCount;
    }

    public void setRecordsErasedCount(Integer recordsErasedCount) {
        this.recordsErasedCount = recordsErasedCount;
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
}
