package com.eneik.epidemiology.document;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dossier_reports")
public class DossierReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, length = 100)
    private String employeeId;

    @Column(name = "template_type", nullable = false, length = 100)
    private String templateType;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "document_count", nullable = false)
    private Integer documentCount;

    @Column(name = "download_url", length = 255)
    private String downloadUrl;

    @Column(name = "access_department", length = 255)
    private String accessDepartment;

    @Column(name = "access_course", length = 255)
    private String accessCourse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public DossierReport() {
    }

    public DossierReport(String employeeId, String templateType, String status, String summaryText, Integer documentCount, String downloadUrl) {
        this.employeeId = employeeId;
        this.templateType = templateType;
        this.status = status;
        this.summaryText = summaryText;
        this.documentCount = documentCount;
        this.downloadUrl = downloadUrl;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getAccessDepartment() {
        return accessDepartment;
    }

    public void setAccessDepartment(String accessDepartment) {
        this.accessDepartment = accessDepartment;
    }

    public String getAccessCourse() {
        return accessCourse;
    }

    public void setAccessCourse(String accessCourse) {
        this.accessCourse = accessCourse;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
