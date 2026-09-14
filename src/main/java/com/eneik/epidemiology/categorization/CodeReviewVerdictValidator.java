package com.eneik.epidemiology.categorization;

import java.util.Set;

/**
 * Validates code review verdicts according to Integration Guardian charter rules.
 * Strictly enforces binary status model: APPROVE or REJECT.
 */
public class CodeReviewVerdictValidator {

    public enum VerdictStatus {
        APPROVE,
        REJECT
    }

    private static final Set<String> DISALLOWED_STATUSES = Set.of("BLOCK", "PENDING", "HOLD", "UNKNOWN");

    public static boolean isValidVerdictStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        if (DISALLOWED_STATUSES.contains(normalized)) {
            return false;
        }
        try {
            VerdictStatus.valueOf(normalized);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String normalizeVerdictStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Review verdict status cannot be null or blank");
        }
        String normalized = status.trim().toUpperCase();
        if (DISALLOWED_STATUSES.contains(normalized) || "BLOCK".equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Status '" + status + "' is invalid. Code review verdicts must strictly use binary status: APPROVE or REJECT");
        }
        try {
            return VerdictStatus.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status '" + status + "' is invalid. Code review verdicts must strictly use binary status: APPROVE or REJECT");
        }
    }
}
