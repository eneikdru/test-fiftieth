package com.eneik.epidemiology.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HazardCategoryRepository extends JpaRepository<HazardCategory, Long> {
    Optional<HazardCategory> findByCode(String code);
}
