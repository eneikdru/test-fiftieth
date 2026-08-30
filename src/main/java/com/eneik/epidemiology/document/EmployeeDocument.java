package com.eneik.epidemiology.document;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_documents")
public class EmployeeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonProperty("employee_id")
    @Column(name = "employee_id", nullable = false, length = 100)
    private String employeeId;

    @com.fasterxml.jackson.annotation.JsonProperty("employee_surname")
    @Column(name = "employee_surname", length = 255)
    private String employeeSurname;

    @com.fasterxml.jackson.annotation.JsonProperty("doc_type")
    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(nullable = false, length = 255)
    private String title;

    @com.fasterxml.jackson.annotation.JsonProperty("doc_date")
    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @Column(columnDefinition = "TEXT")
    private String details;

    @com.fasterxml.jackson.annotation.JsonProperty("scientific_direction")
    @Column(name = "scientific_direction", length = 255)
    private String scientificDirection;

    @com.fasterxml.jackson.annotation.JsonProperty("access_department")
    @Column(name = "access_department", length = 255)
    private String accessDepartment;

    @com.fasterxml.jackson.annotation.JsonProperty("access_course")
    @Column(name = "access_course", length = 255)
    private String accessCourse;

    @com.fasterxml.jackson.annotation.JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public EmployeeDocument() {
    }

    public EmployeeDocument(String employeeId, String docType, String title, LocalDate docDate, String details) {
        this.employeeId = employeeId;
        this.docType = docType;
        this.title = title;
        this.docDate = docDate;
        this.details = details;
    }

    public EmployeeDocument(String employeeId, String docType, String title, LocalDate docDate, String details, String scientificDirection) {
        this.employeeId = employeeId;
        this.docType = docType;
        this.title = title;
        this.docDate = docDate;
        this.details = details;
        this.scientificDirection = scientificDirection;
    }

    public EmployeeDocument(String employeeId, String employeeSurname, String docType, String title, LocalDate docDate, String details) {
        this.employeeId = employeeId;
        this.employeeSurname = employeeSurname;
        this.docType = docType;
        this.title = title;
        this.docDate = docDate;
        this.details = details;
    }

    public EmployeeDocument(String employeeId, String employeeSurname, String docType, String title, LocalDate docDate, String details, String scientificDirection) {
        this.employeeId = employeeId;
        this.employeeSurname = employeeSurname;
        this.docType = docType;
        this.title = title;
        this.docDate = docDate;
        this.details = details;
        this.scientificDirection = scientificDirection;
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

    public String getEmployeeSurname() {
        return employeeSurname;
    }

    public void setEmployeeSurname(String employeeSurname) {
        this.employeeSurname = employeeSurname;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDocDate() {
        return docDate;
    }

    public void setDocDate(LocalDate docDate) {
        this.docDate = docDate;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getScientificDirection() {
        return scientificDirection;
    }

    public void setScientificDirection(String scientificDirection) {
        this.scientificDirection = scientificDirection;
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
