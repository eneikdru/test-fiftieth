package com.eneik.epidemiology.strain;

import java.time.LocalDate;

public class StrainRequestDto {

    private String name;
    private String description;
    private LocalDate identifiedDate;
    private String originCountry;
    private String severityLevel;
    private String accessDepartment;
    private String accessCourse;

    public StrainRequestDto() {
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
