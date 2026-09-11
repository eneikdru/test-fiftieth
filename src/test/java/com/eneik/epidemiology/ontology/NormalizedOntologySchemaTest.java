package com.eneik.epidemiology.ontology;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportDocument;
import com.eneik.epidemiology.document.DossierReportDocumentRepository;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class NormalizedOntologySchemaTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private HazardCategoryRepository hazardCategoryRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportDocumentRepository dossierReportDocumentRepository;

    @Test
    void testNormalizedOntologyTablesSeeded() {
        List<Organization> orgs = organizationRepository.findAll();
        assertThat(orgs).isNotEmpty();
        assertThat(orgs).extracting(Organization::getCode)
                .contains("NII_EPI", "CENTER_INF", "MINZDRAV");

        List<DocumentType> docTypes = documentTypeRepository.findAll();
        assertThat(docTypes).isNotEmpty();
        assertThat(docTypes).extracting(DocumentType::getCode)
                .contains("ORDER", "REPORT", "EXAM", "EXTRACT", "DOSSIER_ENTRY");

        List<HazardCategory> hazards = hazardCategoryRepository.findAll();
        assertThat(hazards).isNotEmpty();
        assertThat(hazards).extracting(HazardCategory::getCode)
                .contains("RESPIRATORY", "FOODBORNE", "ZOONOTIC", "BLOODBORNE");
    }

    @Test
    @Transactional
    void testDossierReportDocumentPartWholeForeignKeyIntegrity() {
        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployeeId("EMP-ONT-001");
        doc.setDocType("REPORT");
        doc.setTitle("Отчет эпидемиологического досье");
        doc.setDocDate(LocalDate.now());
        doc.setDetails("Детали эпидотчета");
        doc = employeeDocumentRepository.save(doc);

        DossierReport report = new DossierReport();
        report.setEmployeeId("EMP-ONT-001");
        report.setTemplateType("FULL_DOSSIER");
        report.setStatus("COMPLETED");
        report.setSummaryText("Досье сотрудника");
        report.setDocumentCount(1);
        report = dossierReportRepository.save(report);

        DossierReportDocument link = new DossierReportDocument(report, doc);
        dossierReportDocumentRepository.save(link);

        List<DossierReportDocument> links = dossierReportDocumentRepository.findByDossierReportId(report.getId());
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getEmployeeDocument().getId()).isEqualTo(doc.getId());

        // Test cascade delete on aggregate root (DossierReport) deletion
        dossierReportRepository.delete(report);
        dossierReportRepository.flush();

        List<DossierReportDocument> linksAfterDelete = dossierReportDocumentRepository.findByDossierReportId(report.getId());
        assertThat(linksAfterDelete).isEmpty();

        // Ensure EmployeeDocument itself remains
        assertThat(employeeDocumentRepository.findById(doc.getId())).isPresent();
    }
}
