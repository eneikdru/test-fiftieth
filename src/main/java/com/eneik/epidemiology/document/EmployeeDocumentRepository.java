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
}
