package com.eneik.epidemiology.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT r FROM DossierReport r WHERE " +
           "(:employeeId IS NULL OR r.employeeId = :employeeId) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:isAdmin = true OR " +
           " (r.accessDepartment IS NULL AND r.accessCourse IS NULL) OR " +
           " (:userDepartment IS NOT NULL AND r.accessDepartment = :userDepartment) OR " +
           " (:userCourses IS NOT NULL AND r.accessCourse IS NOT NULL AND :userCourses LIKE CONCAT('%', r.accessCourse, '%')))")
    Page<DossierReport> searchDossierReportsSecure(
            @Param("employeeId") String employeeId,
            @Param("status") String status,
            @Param("isAdmin") boolean isAdmin,
            @Param("userDepartment") String userDepartment,
            @Param("userCourses") String userCourses,
            Pageable pageable);
}
