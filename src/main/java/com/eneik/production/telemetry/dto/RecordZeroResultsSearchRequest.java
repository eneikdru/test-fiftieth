package com.eneik.production.telemetry.dto;

import jakarta.validation.constraints.NotNull;

public class RecordZeroResultsSearchRequest {

    @NotNull(message = "Search query cannot be null")
    private String searchQuery;

    private String userId;

    public RecordZeroResultsSearchRequest() {
    }

    public RecordZeroResultsSearchRequest(String searchQuery, String userId) {
        this.searchQuery = searchQuery;
        this.userId = userId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
