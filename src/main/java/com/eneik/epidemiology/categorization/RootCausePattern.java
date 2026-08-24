package com.eneik.epidemiology.categorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "root_cause_patterns")
public class RootCausePattern {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "pattern_name", nullable = false, length = 128)
    private String patternName;

    @Column(name = "stream_name", nullable = false, length = 64)
    private String streamName;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "invariant_pattern_id", nullable = false, length = 64)
    private String invariantPatternId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public RootCausePattern() {
    }

    public RootCausePattern(String id, String patternName, String streamName, String ruleCode, String invariantPatternId, OffsetDateTime createdAt) {
        this.id = id;
        this.patternName = patternName;
        this.streamName = streamName;
        this.ruleCode = ruleCode;
        this.invariantPatternId = invariantPatternId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getInvariantPatternId() {
        return invariantPatternId;
    }

    public void setInvariantPatternId(String invariantPatternId) {
        this.invariantPatternId = invariantPatternId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RootCausePattern that = (RootCausePattern) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
