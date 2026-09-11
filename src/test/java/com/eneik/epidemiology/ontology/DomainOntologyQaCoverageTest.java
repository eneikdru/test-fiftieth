package com.eneik.epidemiology.ontology;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportDocument;
import com.eneik.epidemiology.document.DossierReportDocumentRepository;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.privacy.DataErasureToken;
import com.eneik.epidemiology.privacy.DataErasureTokenRepository;
import com.eneik.epidemiology.telemetry.TelemetryEvent;
import com.eneik.epidemiology.telemetry.TelemetryEventRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class DomainOntologyQaCoverageTest {

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

    @Autowired
    private TelemetryEventRepository telemetryEventRepository;

    @Autowired
    private DataErasureTokenRepository dataErasureTokenRepository;

    @Test
    @DisplayName("Given persistent domain model, When database queries execute, Then normalized ontology entities are correctly written and read without mocks")
    @Transactional
    void testPersistentDomainModelNormalizedDataOperations() {
        // Read seeded taxonomy
        List<Organization> orgs = organizationRepository.findAll();
        assertThat(orgs).isNotEmpty();
        assertThat(orgs).extracting(Organization::getCode).contains("NII_EPI", "CENTER_INF", "MINZDRAV");

        List<DocumentType> docTypes = documentTypeRepository.findAll();
        assertThat(docTypes).isNotEmpty();
        assertThat(docTypes).extracting(DocumentType::getCode).contains("ORDER", "REPORT", "EXAM");

        List<HazardCategory> hazards = hazardCategoryRepository.findAll();
        assertThat(hazards).isNotEmpty();
        assertThat(hazards).extracting(HazardCategory::getCode).contains("RESPIRATORY", "FOODBORNE");

        // Write new normalized entity
        Organization customOrg = new Organization("TEST_ORG_01", "Институт экспериментальной гигиены", "Тестовый научно-исследовательский институт");
        Organization savedOrg = organizationRepository.save(customOrg);
        assertThat(savedOrg.getId()).isNotNull();

        Optional<Organization> fetchedOrg = organizationRepository.findByCode("TEST_ORG_01");
        assertThat(fetchedOrg).isPresent();
        assertThat(fetchedOrg.get().getName()).isEqualTo("Институт экспериментальной гигиены");
    }

    @Test
    @DisplayName("Given Dossier aggregate root, When component documents are composed, Then part-whole aggregate integrity is strictly maintained")
    @Transactional
    void testDossierAggregateRootPartWholeIntegrity() {
        EmployeeDocument doc1 = new EmployeeDocument();
        doc1.setEmployeeId("EMP-QA-777");
        doc1.setDocType("REPORT");
        doc1.setTitle("Первичный осмотр очага");
        doc1.setDocDate(LocalDate.of(2026, 9, 11));
        doc1.setDetails("Детали обследования");
        doc1 = employeeDocumentRepository.save(doc1);

        EmployeeDocument doc2 = new EmployeeDocument();
        doc2.setEmployeeId("EMP-QA-777");
        doc2.setDocType("EXAM");
        doc2.setTitle("Заключение лабораторных анализов");
        doc2.setDocDate(LocalDate.of(2026, 9, 11));
        doc2.setDetails("Лабораторные результаты");
        doc2 = employeeDocumentRepository.save(doc2);

        DossierReport aggregateRoot = new DossierReport();
        aggregateRoot.setEmployeeId("EMP-QA-777");
        aggregateRoot.setTemplateType("FULL_EPIDEMIOLOGY_DOSSIER");
        aggregateRoot.setStatus("CREATED");
        aggregateRoot.setSummaryText("Сводный отчет очага заболевания");
        aggregateRoot.setDocumentCount(2);
        aggregateRoot = dossierReportRepository.save(aggregateRoot);

        DossierReportDocument link1 = new DossierReportDocument(aggregateRoot, doc1);
        DossierReportDocument link2 = new DossierReportDocument(aggregateRoot, doc2);
        dossierReportDocumentRepository.saveAll(List.of(link1, link2));

        List<DossierReportDocument> aggregateLinks = dossierReportDocumentRepository.findByDossierReportId(aggregateRoot.getId());
        assertThat(aggregateLinks).hasSize(2);
        assertThat(aggregateLinks).extracting(l -> l.getEmployeeDocument().getId()).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());

        // Deleting aggregate root cascades to component links
        dossierReportRepository.delete(aggregateRoot);
        dossierReportRepository.flush();

        List<DossierReportDocument> linksAfterDelete = dossierReportDocumentRepository.findByDossierReportId(aggregateRoot.getId());
        assertThat(linksAfterDelete).isEmpty();

        // Component documents persist independently
        assertThat(employeeDocumentRepository.findById(doc1.getId())).isPresent();
        assertThat(employeeDocumentRepository.findById(doc2.getId())).isPresent();
    }

    @Test
    @DisplayName("Given bounded contexts, When tested across domain boundaries, Then context models remain isolated without bleed")
    @Transactional
    void testBoundedContextIsolation() {
        long initialOrgCount = organizationRepository.count();
        long initialTelemetryCount = telemetryEventRepository.count();
        long initialPrivacyTokenCount = dataErasureTokenRepository.count();

        // 1. Ontology context transaction
        Organization org = new Organization("BC_ORG_01", "Организация контекста онтологии", "Изолированная сущность онтологии");
        organizationRepository.save(org);

        // 2. Telemetry context transaction
        TelemetryEvent telemetry = new TelemetryEvent("DOSSIER_VIEW", "EMP-QA-777", null, 1, OffsetDateTime.now());
        telemetryEventRepository.save(telemetry);

        // 3. Privacy context transaction
        DataErasureToken privacyToken = new DataErasureToken();
        privacyToken.setSubjectId("EMP-PRIVACY-100");
        privacyToken.setToken(UUID.randomUUID().toString());
        privacyToken.setCreatedAt(OffsetDateTime.now());
        privacyToken.setExpiresAt(OffsetDateTime.now().plusHours(24));
        privacyToken.setUsed(false);
        dataErasureTokenRepository.save(privacyToken);

        // Assert strictly isolated entity state counts incremented by 1 per context
        assertThat(organizationRepository.count()).isEqualTo(initialOrgCount + 1);
        assertThat(telemetryEventRepository.count()).isEqualTo(initialTelemetryCount + 1);
        assertThat(dataErasureTokenRepository.count()).isEqualTo(initialPrivacyTokenCount + 1);

        // Verify entities exist strictly in their own bounded context repositories
        assertThat(organizationRepository.findByCode("BC_ORG_01")).isPresent();
        assertThat(telemetryEventRepository.findAll()).extracting(TelemetryEvent::getEventType).contains("DOSSIER_VIEW");
        assertThat(dataErasureTokenRepository.findByToken(privacyToken.getToken())).isPresent();
    }
}
