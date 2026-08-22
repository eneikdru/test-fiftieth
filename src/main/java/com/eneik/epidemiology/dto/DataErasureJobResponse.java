package com.eneik.epidemiology.dto;

import com.eneik.epidemiology.domain.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataErasureJobResponse {

    @JsonProperty("request_id")
    private UUID requestId;

    @JsonProperty("subject_id")
    private String subjectId;

    @JsonProperty("status")
    private JobStatus status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @JsonProperty("records_erased_count")
    private Integer recordsErasedCount;

    @JsonProperty("error_details")
    private ErrorDetails errorDetails;

    public DataErasureJobResponse() {}

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
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

    public Integer getRecordsErasedCount() {
        return recordsErasedCount;
    }

    public void setRecordsErasedCount(Integer recordsErasedCount) {
        this.recordsErasedCount = recordsErasedCount;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }
}
