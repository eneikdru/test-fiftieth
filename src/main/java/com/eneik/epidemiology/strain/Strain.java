package com.eneik.epidemiology.strain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "strains")
public class Strain {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "identified_date")
    private LocalDate identifiedDate;

    @Column(name = "origin_country")
    private String originCountry;

    @Column(name = "severity_level")
    private String severityLevel;

    @Column(name = "access_department")
    private String accessDepartment;

    @Column(name = "access_course")
    private String accessCourse;

    public Strain() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getIdentifiedDate() {
        return identifiedDate;
    }

    public void setIdentifiedDate(LocalDate identifiedDate) {
        this.identifiedDate = identifiedDate;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
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
}
