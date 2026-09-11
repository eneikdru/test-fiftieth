package com.eneik.epidemiology.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "dossier_report_documents",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_dossier_report_employee_doc", columnNames = {"dossier_report_id", "employee_document_id"})
    }
)
public class DossierReportDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_report_id", nullable = false)
    private DossierReport dossierReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_document_id", nullable = false)
    private EmployeeDocument employeeDocument;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public DossierReportDocument() {
    }

    public DossierReportDocument(DossierReport dossierReport, EmployeeDocument employeeDocument) {
        this.dossierReport = dossierReport;
        this.employeeDocument = employeeDocument;
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

    public DossierReport getDossierReport() {
        return dossierReport;
    }

    public void setDossierReport(DossierReport dossierReport) {
        this.dossierReport = dossierReport;
    }

    public EmployeeDocument getEmployeeDocument() {
        return employeeDocument;
    }

    public void setEmployeeDocument(EmployeeDocument employeeDocument) {
        this.employeeDocument = employeeDocument;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
