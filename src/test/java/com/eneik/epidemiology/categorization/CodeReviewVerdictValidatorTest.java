package com.eneik.epidemiology.categorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeReviewVerdictValidatorTest {

    @Test
    @DisplayName("Given status 'block', When validated, Then returns false and throws exception on normalization")
    void testBlockStatusIsInvalid() {
        assertFalse(CodeReviewVerdictValidator.isValidVerdictStatus("block"));
        assertFalse(CodeReviewVerdictValidator.isValidVerdictStatus("BLOCK"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            CodeReviewVerdictValidator.normalizeVerdictStatus("block")
        );
        assertTrue(ex.getMessage().contains("binary status: APPROVE or REJECT"));
    }

    @Test
    @DisplayName("Given valid statuses 'APPROVE' and 'REJECT', When validated, Then returns true and normalizes cleanly")
    void testValidBinaryStatuses() {
        assertTrue(CodeReviewVerdictValidator.isValidVerdictStatus("APPROVE"));
        assertTrue(CodeReviewVerdictValidator.isValidVerdictStatus("approve"));
        assertTrue(CodeReviewVerdictValidator.isValidVerdictStatus("REJECT"));
        assertTrue(CodeReviewVerdictValidator.isValidVerdictStatus("reject"));

        assertEquals("APPROVE", CodeReviewVerdictValidator.normalizeVerdictStatus("approve"));
        assertEquals("REJECT", CodeReviewVerdictValidator.normalizeVerdictStatus("REJECT"));
    }

    @Test
    @DisplayName("Given DesignReviewConcern, When status 'block' is set, Then throws IllegalArgumentException")
    void testDesignReviewConcernRejectsBlockStatus() {
        DesignReviewConcern concern = new DesignReviewConcern();
        assertThrows(IllegalArgumentException.class, () -> concern.setStatus("block"));
        assertThrows(IllegalArgumentException.class, () -> concern.setStatus("BLOCK"));
    }
}
