package com.eneik.epidemiology.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RuntimeContractFeatureTest {

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private SixSigmaAuditService sixSigmaAuditService;

    @BeforeEach
    public void setUp() {
        featureRepository.deleteAll();

        // Setup test data
        Feature valuelessFeature = new Feature("feat-1", "proj-a", "orig-1", null, true);
        Feature validFeature = new Feature("feat-2", "proj-a", "orig-2", null, false);
        Feature alreadyDismissed = new Feature("feat-3", "proj-a", "orig-3", java.time.OffsetDateTime.now(), true);
        Feature otherProjectFeature = new Feature("feat-4", "proj-b", "orig-4", null, true);

        featureRepository.save(valuelessFeature);
        featureRepository.save(validFeature);
        featureRepository.save(alreadyDismissed);
        featureRepository.save(otherProjectFeature);
    }

    @Test
    public void deleteValuelessEpicsForProject_softDeletesOnlyValuelessForProject() {
        // Given an epic deletion request
        String projectId = "proj-a";

        // When deleteValuelessEpicsForProject executes
        int updated = featureService.deleteValuelessEpicsForProject(projectId);

        // Then it sets dismissedAt instead of deleting the row
        assertEquals(1, updated, "Should update exactly 1 valueless feature for the project");

        Feature updatedFeature = featureRepository.findById("feat-1").orElseThrow();
        assertNotNull(updatedFeature.getDismissedAt(), "dismissedAt should be set");
        assertTrue(updatedFeature.isValueless());

        Feature unupdatedFeature = featureRepository.findById("feat-2").orElseThrow();
        assertNull(unupdatedFeature.getDismissedAt(), "valid feature should not be dismissed");

        Feature otherProj = featureRepository.findById("feat-4").orElseThrow();
        assertNull(otherProj.getDismissedAt(), "feature in other project should not be dismissed");

        List<Feature> activeFeatures = featureService.getActiveFeatures(projectId);
        assertEquals(1, activeFeatures.size(), "Only valid feature should be active");
        assertEquals("feat-2", activeFeatures.get(0).getId());
    }

    @Test
    public void calculateFullSixSigmaAudit_processesNullWithoutCoercion() {
        // Given a cross-project audit request
        // When calculateFullSixSigmaAudit is called
        BigDecimal result = sixSigmaAuditService.calculateFullSixSigmaAudit();

        // Then it processes targetProjectId==null without coercion
        assertNotNull(result);
        assertEquals(new BigDecimal("99.99966"), result, "Should return cross-project metric");
    }
}
