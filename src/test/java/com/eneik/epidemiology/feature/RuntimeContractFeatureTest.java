package com.eneik.epidemiology.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    public void setUp() {
        featureRepository.deleteAll();

        // Setup test data
        Feature valuelessFeature = new Feature("feat-1", "proj-a", "orig-1", null, true);
        Feature validFeature = new Feature("feat-2", "proj-a", "orig-2", null, false);
        Feature alreadyDismissed = new Feature("feat-3", "proj-a", "orig-3", OffsetDateTime.now(), true);
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
    public void deleteValuelessEpics_cascadesSoftDeleteToChildEntities() {
        // Given a project with a valueless epic and child features
        String projectId = "proj-cascade";
        Feature parentEpic = new Feature("epic-100", projectId, null, null, true);
        Feature child1 = new Feature("child-101", projectId, "epic-100", null, false);
        Feature child2 = new Feature("child-102", projectId, "epic-100", null, false);

        featureRepository.save(parentEpic);
        featureRepository.save(child1);
        featureRepository.save(child2);

        // When deleteValuelessEpicsForProject is called
        int totalUpdated = featureService.deleteValuelessEpicsForProject(projectId);

        // Then both the parent epic and child features are soft-deleted (3 total)
        assertEquals(3, totalUpdated, "Should soft delete 1 parent epic and 2 child features");

        Feature fetchedEpic = featureRepository.findById("epic-100").orElseThrow();
        Feature fetchedChild1 = featureRepository.findById("child-101").orElseThrow();
        Feature fetchedChild2 = featureRepository.findById("child-102").orElseThrow();

        assertNotNull(fetchedEpic.getDismissedAt(), "Parent epic dismissedAt should be populated");
        assertNotNull(fetchedChild1.getDismissedAt(), "Child feature 1 dismissedAt should be populated");
        assertNotNull(fetchedChild2.getDismissedAt(), "Child feature 2 dismissedAt should be populated");

        List<Feature> activeFeatures = featureService.getActiveFeatures(projectId);
        assertTrue(activeFeatures.isEmpty(), "No active features should remain in the project");
    }

    @Test
    public void restoreEpic_restoresEpicAndChildEntities() {
        // Given a soft-deleted epic and child features
        String projectId = "proj-restore";
        Feature parentEpic = new Feature("epic-200", projectId, null, OffsetDateTime.now(), true);
        Feature child1 = new Feature("child-201", projectId, "epic-200", OffsetDateTime.now(), false);
        Feature child2 = new Feature("child-202", projectId, "epic-200", OffsetDateTime.now(), false);

        featureRepository.save(parentEpic);
        featureRepository.save(child1);
        featureRepository.save(child2);

        // When restoring the parent epic
        int restoredCount = featureService.restoreEpic("epic-200");

        // Then the epic and its children are restored
        assertEquals(3, restoredCount, "Should restore 1 parent epic and 2 child features");

        Feature restoredEpic = featureRepository.findById("epic-200").orElseThrow();
        Feature restoredChild1 = featureRepository.findById("child-201").orElseThrow();
        Feature restoredChild2 = featureRepository.findById("child-202").orElseThrow();

        assertNull(restoredEpic.getDismissedAt(), "Parent epic dismissedAt should be cleared");
        assertNull(restoredChild1.getDismissedAt(), "Child 1 dismissedAt should be cleared");
        assertNull(restoredChild2.getDismissedAt(), "Child 2 dismissedAt should be cleared");

        List<Feature> activeFeatures = featureService.getActiveFeatures(projectId);
        assertEquals(3, activeFeatures.size(), "Restored epic and children should be returned as active");
    }
}
