package com.eneik.epidemiology.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class DataErasureRequest {

    @NotBlank(message = "ID субъекта данных обязателен")
    @JsonProperty("subject_id")
    private String subjectId;

    @NotBlank(message = "Токен подтверждения обязателен")
    @JsonProperty("confirmation_token")
    private String confirmationToken;

    @NotBlank(message = "Причина удаления обязательна")
    @JsonProperty("reason")
    private String reason;

    @JsonProperty("erasure_scope")
    private String erasureScope = "ALL_PERSONAL_DATA";

    public DataErasureRequest() {}

    public DataErasureRequest(String subjectId, String confirmationToken, String reason, String erasureScope) {
        this.subjectId = subjectId;
        this.confirmationToken = confirmationToken;
        this.reason = reason;
        this.erasureScope = erasureScope;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getErasureScope() {
        return erasureScope;
    }

    public void setErasureScope(String erasureScope) {
        this.erasureScope = erasureScope;
    }
}
