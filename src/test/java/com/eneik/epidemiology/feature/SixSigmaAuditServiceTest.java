package com.eneik.epidemiology.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class SixSigmaAuditServiceTest {

    @Autowired
    private SixSigmaAuditService sixSigmaAuditService;

    @Test
    @DisplayName("Given a request to calculate a full Six Sigma audit, When calculateFullSixSigmaAudit is invoked, Then it calculates single-project metric value for the active project")
    void testCalculateFullSixSigmaAudit_bypassesProjectCoercion() {
        BigDecimal result = sixSigmaAuditService.calculateFullSixSigmaAudit();

        assertNotNull(result, "Result must not be null");
        assertEquals(new BigDecimal("95.00000"), result, "Full audit must return active project metric value");
    }

    @Test
    @DisplayName("Given a project ID, When calculateProjectSixSigmaAudit is called, Then it calculates single-project metric value")
    void testCalculateProjectSixSigmaAudit_withSpecificProject() {
        BigDecimal result = sixSigmaAuditService.calculateProjectSixSigmaAudit("proj-test-1");

        assertNotNull(result, "Result must not be null");
        assertEquals(new BigDecimal("95.00000"), result, "Project audit must return specific project metric value");
    }

    @Test
    @DisplayName("Given null project ID, When calculateProjectSixSigmaAudit is called, Then it coerces project ID to active project")
    void testCalculateProjectSixSigmaAudit_withNullProject() {
        BigDecimal result = sixSigmaAuditService.calculateProjectSixSigmaAudit(null);

        assertNotNull(result, "Result must not be null");
        assertEquals(new BigDecimal("95.00000"), result, "Project audit with null should coerce to active project");
    }
}
