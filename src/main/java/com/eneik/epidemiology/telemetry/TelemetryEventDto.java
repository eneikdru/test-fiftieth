package com.eneik.epidemiology.telemetry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class TelemetryEventDto {

    private Long id;

    @JsonProperty("eventType")
    @JsonAlias("event_type")
    private String eventType;

    @JsonProperty("queryTerm")
    @JsonAlias("query_term")
    private String queryTerm;

    @JsonProperty("documentId")
    @JsonAlias("document_id")
    private Long documentId;

    @JsonProperty("resultsCount")
    @JsonAlias("results_count")
    private Integer resultsCount;

    @JsonProperty("processingTimeMs")
    @JsonAlias("processing_time_ms")
    private Long processingTimeMs;

    @JsonProperty("workflowDurationMs")
    @JsonAlias("workflow_duration_ms")
    private Long workflowDurationMs;

    private String module;
    private String title;
    private Boolean success;

    @JsonProperty("startTime")
    @JsonAlias("start_time")
    private OffsetDateTime startTime;

    @JsonProperty("endTime")
    @JsonAlias("end_time")
    private OffsetDateTime endTime;

    @JsonProperty("createdAt")
    @JsonAlias("created_at")
    private OffsetDateTime createdAt;

    public TelemetryEventDto() {
    }

    public TelemetryEventDto(TelemetryEvent entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.eventType = entity.getEventType();
            this.queryTerm = entity.getQueryTerm();
            this.documentId = entity.getDocumentId();
            this.resultsCount = entity.getResultsCount();
            this.processingTimeMs = entity.getProcessingTimeMs();
            this.workflowDurationMs = entity.getWorkflowDurationMs();
            this.module = entity.getModule();
            this.title = entity.getTitle();
            this.success = entity.getSuccess();
            this.startTime = entity.getStartTime();
            this.endTime = entity.getEndTime();
            this.createdAt = entity.getCreatedAt();
        }
    }

    public TelemetryEvent toEntity() {
        TelemetryEvent entity = new TelemetryEvent();
        entity.setId(this.id);
        entity.setEventType(this.eventType);
        entity.setQueryTerm(this.queryTerm);
        entity.setDocumentId(this.documentId);
        entity.setResultsCount(this.resultsCount);
        entity.setProcessingTimeMs(this.processingTimeMs);
        entity.setWorkflowDurationMs(this.workflowDurationMs);
        entity.setModule(this.module);
        entity.setTitle(this.title);
        entity.setSuccess(this.success);
        entity.setStartTime(this.startTime);
        entity.setEndTime(this.endTime);
        entity.setCreatedAt(this.createdAt);
        return entity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getQueryTerm() {
        return queryTerm;
    }

    public void setQueryTerm(String queryTerm) {
        this.queryTerm = queryTerm;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getResultsCount() {
        return resultsCount;
    }

    public void setResultsCount(Integer resultsCount) {
        this.resultsCount = resultsCount;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Long getWorkflowDurationMs() {
        return workflowDurationMs;
    }

    public void setWorkflowDurationMs(Long workflowDurationMs) {
        this.workflowDurationMs = workflowDurationMs;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
