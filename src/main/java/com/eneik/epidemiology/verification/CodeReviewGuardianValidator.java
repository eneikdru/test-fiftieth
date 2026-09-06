package com.eneik.epidemiology.verification;

import java.util.List;
import java.util.Set;

/**
 * Service enforcing review verdict criteria and PR diff grounding rules.
 */
public class CodeReviewGuardianValidator {

    public enum ReviewVerdict {
        APPROVE,
        REJECT
    }

    public record ReviewResult(ReviewVerdict verdict, String reason, List<String> invalidEvaluatedFiles) {}

    /**
     * Evaluates a review request against the actual changed files in a PR diff.
     *
     * @param changedFiles actual set of file paths present in the PR diff
     * @param evaluatedFiles set of file paths evaluated in the review
     * @param hasOutOfScopeChanges flag indicating if the PR contains out-of-scope modifications
     * @return ReviewResult with binary verdict, reason, and list of any hallucinated/evaluated files not in diff
     */
    public ReviewResult evaluateReview(Set<String> changedFiles, Set<String> evaluatedFiles, boolean hasOutOfScopeChanges) {
        List<String> invalidFiles = evaluatedFiles.stream()
                .filter(f -> !changedFiles.contains(f))
                .toList();

        if (!invalidFiles.isEmpty()) {
            return new ReviewResult(
                    ReviewVerdict.REJECT,
                    "REJECT: Review evaluated files not present in the PR diff (hallucination detected): " + invalidFiles,
                    invalidFiles
            );
        }

        if (hasOutOfScopeChanges) {
            return new ReviewResult(
                    ReviewVerdict.REJECT,
                    "REJECT: PR contains out-of-scope modifications, violating refusal criteria.",
                    List.of()
            );
        }

        return new ReviewResult(
                ReviewVerdict.APPROVE,
                "APPROVE: PR meets all quality, scope, and diff-grounding criteria.",
                List.of()
        );
    }
}
