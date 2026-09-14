package com.eneik.epidemiology.categorization;

public enum CodeReviewVerdictStatus {
    APPROVE,
    REJECT;

    public static boolean isValidVerdictStatus(String status) {
        if (status == null) {
            return false;
        }
        try {
            CodeReviewVerdictStatus.valueOf(status.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static CodeReviewVerdictStatus parseVerdictStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Review verdict status cannot be null");
        }
        String normalized = status.trim().toUpperCase();
        if ("BLOCK".equals(normalized) || "BLOCKED".equals(normalized)) {
            throw new IllegalArgumentException("Verdict status 'block' is non-binary and prohibited by Code Guardian charter");
        }
        try {
            return CodeReviewVerdictStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid review verdict status '" + status + "'. Must be APPROVE or REJECT");
        }
    }
}
