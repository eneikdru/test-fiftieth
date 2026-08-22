package com.eneik.production.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry_events")
public class TelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private TelemetryEventType eventType;

    @Column(name = "document_id", length = 128)
    private String documentId;

    @Column(name = "search_query", length = 512)
    private String searchQuery;

    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public TelemetryEvent() {
    }

    public TelemetryEvent(TelemetryEventType eventType, String documentId, String searchQuery, String userId, OffsetDateTime createdAt) {
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

    public void setEventType(TelemetryEventType eventType) {
        this.eventType = eventType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
