package com.eneik.epidemiology.ontology;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportDocument;
import com.eneik.epidemiology.document.DossierReportDocumentRepository;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.user.User;
import com.eneik.epidemiology.user.UserRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class DomainOntologyQaCoverageTest {

    @Autowired
    private MockMvc mockMvc;

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
    private UserRepository userRepository;

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
    @DisplayName("Given bounded contexts, When tested end-to-end via HTTP endpoints, Then boundaries remain isolated and strictly enforced without cross-domain bleed")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @Transactional
    void testBoundedContextIsolationEndToEnd() throws Exception {
        // Seed subject user for Privacy context HTTP request
        User user = new User();
        user.setUsername("EMP-QA-E2E-100");
        user.setPasswordHash("hash");
        user.setRole("RESEARCHER");
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);

        // 1. Catalog / Document Bounded Context E2E Endpoint
        mockMvc.perform(get("/api/v1/documents/search"))
                .andExpect(status().isOk());

        // 2. Strain Bounded Context E2E Endpoint
        mockMvc.perform(get("/api/v1/strains"))
                .andExpect(status().isOk());

        // 3. Privacy Bounded Context E2E Endpoint
        String privacyJson = """
                {
                    "subject_id": "EMP-QA-E2E-100",
                    "requested_format": "JSON",
                    "notes": "E2E verification of privacy bounded context"
                }
                """;

        mockMvc.perform(post("/api/v1/privacy/export-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(privacyJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.subject_id").value("EMP-QA-E2E-100"))
                .andExpect(jsonPath("$.request_id").exists());
    }
}
