package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.categorization.CodeReviewVerdictStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TaskCc0572f7RecoveryVerificationTest {

    @ParameterizedTest
    @ValueSource(strings = {"APPROVE", "REJECT", "approve", "reject", " APPROVE "})
    @DisplayName("Given valid binary review verdict status, When validated, Then returns true")
    void testValidBinaryVerdictStatuses(String status) {
        assertTrue(CodeReviewVerdictStatus.isValidVerdictStatus(status));
        assertNotNull(CodeReviewVerdictStatus.parseVerdictStatus(status));
    }

    @ParameterizedTest
    @ValueSource(strings = {"block", "BLOCK", "pending", "unknown", "HOLD", "INVALID"})
    @DisplayName("Given non-binary or invalid review verdict status, When validated, Then returns false")
    void testInvalidNonBinaryVerdictStatuses(String status) {
        assertFalse(CodeReviewVerdictStatus.isValidVerdictStatus(status));
        assertThrows(IllegalArgumentException.class, () -> CodeReviewVerdictStatus.parseVerdictStatus(status));
    }

    @Test
    @DisplayName("Given null review verdict status, When validated, Then returns false safely")
    void testNullVerdictStatus() {
        assertFalse(CodeReviewVerdictStatus.isValidVerdictStatus(null));
        assertThrows(IllegalArgumentException.class, () -> CodeReviewVerdictStatus.parseVerdictStatus(null));
    }
}
