package com.eneik.epidemiology.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuntimeContractRepository extends JpaRepository<RuntimeContractConfiguration, String> {
}
