package com.eneik.epidemiology.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierReportRepository extends JpaRepository<DossierReport, Long> {

    List<DossierReport> findByEmployeeId(String employeeId);

    @Modifying
    @Query("UPDATE DossierReport d SET d.status = :newStatus WHERE d.id = :id AND d.status = :oldStatus")
    int updateStatus(@Param("id") Long id, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus);
}
