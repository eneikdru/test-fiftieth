package com.eneik.epidemiology.categorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RootCausePatternRepository extends JpaRepository<RootCausePattern, String> {

    Optional<RootCausePattern> findByStreamNameAndRuleCode(String streamName, String ruleCode);

    Optional<RootCausePattern> findByStreamName(String streamName);
}
