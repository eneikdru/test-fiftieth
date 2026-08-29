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
        int updatedCount = featureRepository.softDeleteValuelessEpics(projectId, now);

        log.info("Soft deleted {} valueless epics for project {} by setting dismissedAt", updatedCount, projectId);
        return updatedCount;
    }

    public List<Feature> getActiveFeatures(String projectId) {
        return featureRepository.findByProjectIdAndDismissedAtIsNull(projectId);
    }
}
