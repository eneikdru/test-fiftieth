package com.eneik.epidemiology.categorization;

import java.util.ArrayList;
import java.util.List;

public class CategorizationCoverageResponse {
    private String streamName;
    private long totalConcerns;
    private long categorizedConcerns;
    private double coverageRate;
    private List<String> gaps = new ArrayList<>();

    public CategorizationCoverageResponse() {
    }

    public CategorizationCoverageResponse(String streamName, long totalConcerns, long categorizedConcerns, double coverageRate) {
        this(streamName, totalConcerns, categorizedConcerns, coverageRate, new ArrayList<>());
    }

    public CategorizationCoverageResponse(String streamName, long totalConcerns, long categorizedConcerns, double coverageRate, List<String> gaps) {
        this.streamName = streamName;
        this.totalConcerns = totalConcerns;
        this.categorizedConcerns = categorizedConcerns;
        this.coverageRate = coverageRate;
        this.gaps = gaps != null ? gaps : new ArrayList<>();
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

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps != null ? gaps : new ArrayList<>();
    }
}
