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


    @Modifying(clearAutomatically = true)
    @Query("UPDATE DossierReport d SET d.isSigned = true, d.signature = :signature WHERE d.id = :id AND d.isSigned = false AND d.status = 'COMPLETED'")
    int signReport(@Param("id") Long id, @Param("signature") String signature);

    @Query("SELECT r FROM DossierReport r WHERE " +
           "(:employeeId IS NULL OR r.employeeId = :employeeId) AND " +
           "(:isAdmin = true OR " +
           " (r.accessDepartment IS NULL AND r.accessCourse IS NULL) OR " +
           " (r.accessDepartment = :userDepartment) OR " +
           " (:userCourses IS NOT NULL AND r.accessCourse IS NOT NULL AND LOWER(CAST(:userCourses AS string)) LIKE LOWER(CONCAT('%', CAST(r.accessCourse AS string), '%')))) " +
           "ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<DossierReport> searchReportsSecure(
            @Param("employeeId") String employeeId,
            @Param("isAdmin") boolean isAdmin,
            @Param("userDepartment") String userDepartment,
            @Param("userCourses") String userCourses,
            org.springframework.data.domain.Pageable pageable);
}
