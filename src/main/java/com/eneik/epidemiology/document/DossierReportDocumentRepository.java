package com.eneik.epidemiology.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierReportDocumentRepository extends JpaRepository<DossierReportDocument, Long> {
    List<DossierReportDocument> findByDossierReportId(Long dossierReportId);
    List<DossierReportDocument> findByEmployeeDocumentId(Long employeeDocumentId);
}
