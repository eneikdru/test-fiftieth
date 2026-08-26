package com.eneik.epidemiology.categorization;

public class CategorizationCoverageResponse {
    private String streamName;
    private long totalConcerns;
    private long categorizedConcerns;
    private double coverageRate;

    public CategorizationCoverageResponse() {
    }

    public CategorizationCoverageResponse(String streamName, long totalConcerns, long categorizedConcerns, double coverageRate) {
        this.streamName = streamName;
        this.totalConcerns = totalConcerns;
        this.categorizedConcerns = categorizedConcerns;
        this.coverageRate = coverageRate;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public long getTotalConcerns() {
        return totalConcerns;
    }

    public void setTotalConcerns(long totalConcerns) {
        this.totalConcerns = totalConcerns;
    }

    public long getCategorizedConcerns() {
        return categorizedConcerns;
    }

    public void setCategorizedConcerns(long categorizedConcerns) {
        this.categorizedConcerns = categorizedConcerns;
    }

    public double getCoverageRate() {
        return coverageRate;
    }

    public void setCoverageRate(double coverageRate) {
        this.coverageRate = coverageRate;
    }
}
