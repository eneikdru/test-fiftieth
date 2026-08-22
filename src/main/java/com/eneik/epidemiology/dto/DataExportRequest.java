package com.eneik.epidemiology.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class DataExportRequest {

    @NotBlank(message = "ID субъекта данных обязателен")
    @JsonProperty("subject_id")
    private String subjectId;

    @JsonProperty("requested_format")
    private String requestedFormat = "ZIP";

    @JsonProperty("notes")
    private String notes;

    public DataExportRequest() {}

    public DataExportRequest(String subjectId, String requestedFormat, String notes) {
        this.subjectId = subjectId;
        this.requestedFormat = requestedFormat;
        this.notes = notes;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getRequestedFormat() {
        return requestedFormat;
    }

    public void setRequestedFormat(String requestedFormat) {
        this.requestedFormat = requestedFormat;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
