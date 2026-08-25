package com.eneik.epidemiology.categorization;

import java.util.Map;
import java.util.Objects;

public class ExternalSchemaEvent {

    private String eventId;
    private String streamName;
    private String schemaVersion;
    private Map<String, Object> payload;

    public ExternalSchemaEvent() {
    }

    public ExternalSchemaEvent(String eventId, String streamName, String schemaVersion, Map<String, Object> payload) {
        this.eventId = eventId;
        this.streamName = streamName;
        this.schemaVersion = schemaVersion;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalSchemaEvent that = (ExternalSchemaEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
