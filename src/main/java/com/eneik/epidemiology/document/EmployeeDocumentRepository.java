package com.eneik.epidemiology.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {

    List<EmployeeDocument> findByEmployeeIdOrderByDocDateDesc(String employeeId);

    List<EmployeeDocument> findByEmployeeIdAndDocType(String employeeId, String docType);

    @Query("SELECT d FROM EmployeeDocument d WHERE d.employeeId = :employeeId ORDER BY d.docDate DESC")
    List<EmployeeDocument> findUnifiedEmployeeDossier(@Param("employeeId") String employeeId);

    @Query("SELECT d FROM EmployeeDocument d WHERE (:employeeId IS NULL OR d.employeeId = :employeeId) " +
           "AND (:employeeSurname IS NULL OR d.employeeSurname = :employeeSurname) " +
           "AND (:docType IS NULL OR d.docType = :docType) " +
           "AND (:scientificDirection IS NULL OR d.scientificDirection = :scientificDirection) " +
           "AND (:applyFilter = false OR d.docType NOT IN ('STRAIN_ISOLATION', 'REPORT') OR (d.accessDepartment IS NULL AND d.accessCourse IS NULL) OR (:department IS NOT NULL AND d.accessDepartment = :department) OR (:course IS NOT NULL AND d.accessCourse IS NOT NULL AND d.accessCourse LIKE CONCAT('%', :course, '%'))) AND (:query IS NULL OR LOWER(CAST(d.title AS string)) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR LOWER(CAST(d.details AS string)) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) " +
           "AND (CAST(:fromDate AS java.time.LocalDate) IS NULL OR d.docDate >= :fromDate) " +
           "AND (CAST(:toDate AS java.time.LocalDate) IS NULL OR d.docDate <= :toDate) " +
           "ORDER BY d.docDate DESC")
    org.springframework.data.domain.Page<EmployeeDocument> searchEmployeeDocuments(
            @Param("employeeId") String employeeId,
            @Param("employeeSurname") String employeeSurname,
            @Param("docType") String docType,
            @Param("scientificDirection") String scientificDirection,
            @Param("query") String query,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate,
            @Param("applyFilter") boolean applyFilter,
            @Param("department") String department,
            @Param("course") String course,
            org.springframework.data.domain.Pageable pageable
    );
}
