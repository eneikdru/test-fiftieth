package com.eneik.epidemiology.telemetry;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry_events")
public class TelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "query_term", length = 255)
    private String queryTerm;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "results_count")
    private Integer resultsCount;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "workflow_duration_ms")
    private Long workflowDurationMs;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public TelemetryEvent() {
    }

    public TelemetryEvent(String eventType, String queryTerm, Long documentId, Integer resultsCount, OffsetDateTime createdAt) {
        this.eventType = eventType;
        this.queryTerm = queryTerm;
        this.documentId = documentId;
        this.resultsCount = resultsCount;
        this.createdAt = createdAt;
    }

    public TelemetryEvent(String eventType, String queryTerm, Long documentId, Integer resultsCount, Long processingTimeMs, OffsetDateTime createdAt) {
        this.eventType = eventType;
        this.queryTerm = queryTerm;
        this.documentId = documentId;
        this.resultsCount = resultsCount;
        this.processingTimeMs = processingTimeMs;
        this.createdAt = createdAt;
    }

    public static TelemetryEvent createWorkflowEvent(String eventType, String workflowType, OffsetDateTime startTime, OffsetDateTime endTime, Long workflowDurationMs, OffsetDateTime createdAt) {
        TelemetryEvent event = new TelemetryEvent();
        event.setEventType(eventType);
        event.setQueryTerm(workflowType);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setWorkflowDurationMs(workflowDurationMs);
        event.setCreatedAt(createdAt);
        return event;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
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
}
