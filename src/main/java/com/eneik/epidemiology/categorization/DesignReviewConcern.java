package com.eneik.epidemiology.categorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "design_review_concerns")
public class DesignReviewConcern {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "stream_name", nullable = false, length = 64)
    private String streamName;

    @Column(name = "epic_sequence", nullable = false)
    private Integer epicSequence;

    @Column(name = "u_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal uValue;

    @Column(name = "root_cause_pattern_id", length = 64)
    private String rootCausePatternId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public DesignReviewConcern() {
    }

    public DesignReviewConcern(String id, String streamName, Integer epicSequence, BigDecimal uValue, String rootCausePatternId, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.streamName = streamName;
        this.epicSequence = epicSequence;
        this.uValue = uValue;
        this.rootCausePatternId = rootCausePatternId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public Integer getEpicSequence() {
        return epicSequence;
    }

    public void setEpicSequence(Integer epicSequence) {
        this.epicSequence = epicSequence;
    }

    public BigDecimal getuValue() {
        return uValue;
    }

    public void setuValue(BigDecimal uValue) {
        this.uValue = uValue;
    }

    public String getRootCausePatternId() {
        return rootCausePatternId;
    }

    public void setRootCausePatternId(String rootCausePatternId) {
        this.rootCausePatternId = rootCausePatternId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        DesignReviewConcern that = (DesignReviewConcern) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
