package com.eneik.epidemiology.categorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignReviewConcernRepository extends JpaRepository<DesignReviewConcern, String> {

    List<DesignReviewConcern> findByStreamNameAndRootCausePatternIdIsNull(String streamName);

    @Modifying
    @Query("UPDATE DesignReviewConcern c SET c.rootCausePatternId = :patternId, c.status = 'CATEGORIZED' WHERE c.id = :id AND c.rootCausePatternId IS NULL")
    int categorizeConcernAtomically(@Param("id") String id, @Param("patternId") String patternId);
}
