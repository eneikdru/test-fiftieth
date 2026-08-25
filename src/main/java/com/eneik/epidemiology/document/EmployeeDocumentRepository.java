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
           "AND (:query IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.details) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (cast(:fromDate as date) IS NULL OR d.docDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR d.docDate <= :toDate) " +
           "ORDER BY d.docDate DESC")
    List<EmployeeDocument> searchEmployeeDocuments(
            @Param("employeeId") String employeeId,
            @Param("employeeSurname") String employeeSurname,
            @Param("docType") String docType,
            @Param("scientificDirection") String scientificDirection,
            @Param("query") String query,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate
    );
}
