package com.eneik.epidemiology.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "features")
public class Feature {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "origin_feature_id")
    private String originFeatureId;

    @Column(name = "dismissed_at")
    private OffsetDateTime dismissedAt;

    @Column(name = "valueless", nullable = false)
    private boolean valueless;

    public Feature() {
    }

    public Feature(String id, String projectId, String originFeatureId, OffsetDateTime dismissedAt, boolean valueless) {
        this.id = id;
        this.projectId = projectId;
        this.originFeatureId = originFeatureId;
        this.dismissedAt = dismissedAt;
        this.valueless = valueless;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getOriginFeatureId() {
        return originFeatureId;
    }

    public void setOriginFeatureId(String originFeatureId) {
        this.originFeatureId = originFeatureId;
    }

    public OffsetDateTime getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(OffsetDateTime dismissedAt) {
        this.dismissedAt = dismissedAt;
    }

    public boolean isValueless() {
        return valueless;
    }

    public void setValueless(boolean valueless) {
        this.valueless = valueless;
    }
}
