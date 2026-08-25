package com.eneik.epidemiology.categorization;

public class CategorizationResult {
    private int categorizedCount;

    public CategorizationResult() {}

    public CategorizationResult(int categorizedCount) {
        this.categorizedCount = categorizedCount;
    }

    public int getCategorizedCount() {
        return categorizedCount;
    }

    public void setCategorizedCount(int categorizedCount) {
        this.categorizedCount = categorizedCount;
    }
}
