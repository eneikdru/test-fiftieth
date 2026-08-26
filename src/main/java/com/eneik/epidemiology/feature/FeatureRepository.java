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
    @Query("UPDATE Feature f SET f.dismissedAt = :dismissedAt WHERE f.projectId = :projectId AND f.valueless = true AND f.dismissedAt IS NULL")
    int softDeleteValuelessEpics(@Param("projectId") String projectId, @Param("dismissedAt") OffsetDateTime dismissedAt);

    List<Feature> findByProjectIdAndDismissedAtIsNull(String projectId);
}
