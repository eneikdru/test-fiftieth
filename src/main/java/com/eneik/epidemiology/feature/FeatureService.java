package com.eneik.epidemiology.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class FeatureService {

    private static final Logger log = LoggerFactory.getLogger(FeatureService.class);

    private final FeatureRepository featureRepository;

    @Autowired
    public FeatureService(FeatureRepository featureRepository) {
        this.featureRepository = featureRepository;
    }

    @Transactional
    public int deleteValuelessEpicsForProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be null or blank");
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<String> epicIds = featureRepository.findValuelessEpicIdsByProjectId(projectId);

        int updatedEpicsCount = featureRepository.softDeleteValuelessEpics(projectId, now);

        int updatedChildrenCount = 0;
        if (!epicIds.isEmpty()) {
            updatedChildrenCount = featureRepository.softDeleteChildrenOfEpics(epicIds, now);
        }

        int totalUpdated = updatedEpicsCount + updatedChildrenCount;
        log.info("Soft deleted {} valueless epics and {} child features for project {}", updatedEpicsCount, updatedChildrenCount, projectId);
        return totalUpdated;
    }

    @Transactional
    public int restoreEpic(String epicId) {
        if (epicId == null || epicId.isBlank()) {
            throw new IllegalArgumentException("epicId must not be null or blank");
        }

        int restoredCount = featureRepository.restoreEpicAndChildren(epicId);
        log.info("Restored epic {} and its children (total rows restored: {})", epicId, restoredCount);
        return restoredCount;
    }

    public List<Feature> getActiveFeatures(String projectId) {
        return featureRepository.findByProjectIdAndDismissedAtIsNull(projectId);
    }

    public List<Feature> getChildFeatures(String originFeatureId) {
        return featureRepository.findByOriginFeatureId(originFeatureId);
    }
}
