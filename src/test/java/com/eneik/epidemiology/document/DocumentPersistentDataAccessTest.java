package com.eneik.epidemiology.document;

import com.eneik.epidemiology.ontology.DocumentTypeRepository;
import com.eneik.epidemiology.ontology.HazardCategoryRepository;
import com.eneik.epidemiology.ontology.OrganizationRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@Transactional
class DocumentPersistentDataAccessTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private HazardCategoryRepository hazardCategoryRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Autowired
    private DossierReportDocumentRepository dossierReportDocumentRepository;

    @Test
    @DisplayName("Given persistent data layer, When querying documents, Then entities are retrieved from physical database table")
    void testDocumentPersistenceLayer_QueriesPhysicalDatabase() {
        Document newDoc = new Document("Persistent Test Study", "НИИ Эпидемиологии", 2026, "/data/docs/uploads/persistent.pdf");
        newDoc.setDocType("REPORT");
        newDoc.setTextContent("Эпидемиологический отчет о состоянии очага.");

        Document savedDoc = documentRepository.save(newDoc);
        documentRepository.flush();

        assertThat(savedDoc.getId()).isNotNull();

        Document fetchedDoc = documentRepository.findById(savedDoc.getId()).orElse(null);
        assertThat(fetchedDoc).isNotNull();
        assertThat(fetchedDoc.getTitle()).isEqualTo("Persistent Test Study");
        assertThat(fetchedDoc.getDocType()).isEqualTo("REPORT");
    }

    @Test
    @DisplayName("Given normalized domain ontology repositories, When querying reference data, Then persisted entities exist")
    void testNormalizedDomainOntologyRepositories_ExistInDatastore() {
        assertThat(organizationRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(documentTypeRepository.count()).isGreaterThanOrEqualTo(5);
        assertThat(hazardCategoryRepository.count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Given dossier aggregate root, When saving dossier report with documents, Then structural relationships are persisted")
    void testDossierAggregateRootPersistence() {
        EmployeeDocument employeeDocument = new EmployeeDocument();
        employeeDocument.setEmployeeId("EMP-PERSIST-01");
        employeeDocument.setDocType("REPORT");
        employeeDocument.setTitle("Документ досье");
        employeeDocument.setDocDate(LocalDate.now());
        employeeDocument.setDetails("Детали отчета досье");
        employeeDocument = employeeDocumentRepository.save(employeeDocument);

        DossierReport dossierReport = new DossierReport();
        dossierReport.setEmployeeId("EMP-PERSIST-01");
        dossierReport.setTemplateType("FULL_DOSSIER");
        dossierReport.setStatus("COMPLETED");
        dossierReport.setSummaryText("Сводный отчет досье");
        dossierReport.setDocumentCount(1);
        dossierReport = dossierReportRepository.save(dossierReport);

        DossierReportDocument dossierDoc = new DossierReportDocument(dossierReport, employeeDocument);
        dossierReportDocumentRepository.save(dossierDoc);

        List<DossierReportDocument> links = dossierReportDocumentRepository.findByDossierReportId(dossierReport.getId());
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getEmployeeDocument().getId()).isEqualTo(employeeDocument.getId());
    }
}
