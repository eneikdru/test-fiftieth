package com.eneik.epidemiology.auth;

import java.util.Objects;
import java.util.Optional;

/**
 * Constructive proof object (Result) for OIDC token verification.
 * Disambiguates success states from explicit failure modes without generic null collapse.
 */
public sealed interface OidcTokenValidationResult {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    Optional<OidcProfile> profile();

    Optional<OidcValidationException> failure();

    record OidcProfile(
            String username,
            String moodleRole,
            String department,
            String email,
            String fullName,
            String courses,
            boolean suspended
    ) {}

    record Success(OidcProfile profileData) implements OidcTokenValidationResult {
        public Success {
            Objects.requireNonNull(profileData, "profileData must not be null");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<OidcProfile> profile() {
            return Optional.of(profileData);
        }

        @Override
        public Optional<OidcValidationException> failure() {
            return Optional.empty();
        }
    }

    record Failure(OidcValidationException exception) implements OidcTokenValidationResult {
        public Failure {
            Objects.requireNonNull(exception, "exception must not be null");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<OidcProfile> profile() {
            return Optional.empty();
        }

        @Override
        public Optional<OidcValidationException> failure() {
            return Optional.of(exception);
        }
    }

    static OidcTokenValidationResult success(String username, String moodleRole, String department, String email, String fullName, String courses, boolean suspended) {
        return new Success(new OidcProfile(username, moodleRole, department, email, fullName, courses, suspended));
    }

    static OidcTokenValidationResult failure(OidcValidationException exception) {
        return new Failure(exception);
    }
}
