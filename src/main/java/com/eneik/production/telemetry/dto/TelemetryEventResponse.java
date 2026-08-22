package com.eneik.production.telemetry.dto;

import com.eneik.production.telemetry.TelemetryEventType;

import java.time.OffsetDateTime;

public class TelemetryEventResponse {

    private Long id;
    private TelemetryEventType eventType;
    private String documentId;
    private String searchQuery;
    private String userId;
    private OffsetDateTime createdAt;

    public TelemetryEventResponse() {
    }

    public TelemetryEventResponse(Long id, TelemetryEventType eventType, String documentId, String searchQuery, String userId, OffsetDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.documentId = documentId;
        this.searchQuery = searchQuery;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public TelemetryEventType getEventType() {
        return eventType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getUserId() {
        return userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
