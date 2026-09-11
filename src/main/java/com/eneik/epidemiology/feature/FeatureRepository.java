package com.eneik.epidemiology.feature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "WITH RECURSIVE cascade_features AS (" +
            "SELECT id FROM features WHERE project_id = :projectId AND valueless = true AND dismissed_at IS NULL " +
            "UNION ALL " +
            "SELECT f.id FROM features f " +
            "INNER JOIN cascade_features cf ON f.origin_feature_id = cf.id " +
            "WHERE f.dismissed_at IS NULL) " +
            "UPDATE features SET dismissed_at = :dismissedAt WHERE id IN (SELECT id FROM cascade_features)",
            nativeQuery = true)
    int softDeleteValuelessEpics(@Param("projectId") String projectId, @Param("dismissedAt") OffsetDateTime dismissedAt);

    List<Feature> findByProjectIdAndDismissedAtIsNull(String projectId);
}
