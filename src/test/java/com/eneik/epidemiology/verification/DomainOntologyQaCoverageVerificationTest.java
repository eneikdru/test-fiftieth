package com.eneik.epidemiology.verification;

import com.eneik.epidemiology.document.DossierReport;
import com.eneik.epidemiology.document.DossierReportDocument;
import com.eneik.epidemiology.document.DossierReportDocumentRepository;
import com.eneik.epidemiology.document.DossierReportRepository;
import com.eneik.epidemiology.document.EmployeeDocument;
import com.eneik.epidemiology.document.EmployeeDocumentRepository;
import com.eneik.epidemiology.ontology.DocumentType;
import com.eneik.epidemiology.ontology.DocumentTypeRepository;
import com.eneik.epidemiology.ontology.HazardCategory;
import com.eneik.epidemiology.ontology.HazardCategoryRepository;
import com.eneik.epidemiology.ontology.Organization;
import com.eneik.epidemiology.ontology.OrganizationRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Disabled;
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
@Disabled("Shift-Left QA test suite pending backend implementation")
class DomainOntologyQaCoverageVerificationTest {

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
    private com.eneik.epidemiology.user.UserRepository userRepository;

    @Test
    @Transactional
    @DisplayName("Given persistent domain model, When reading and writing normalized ontology data, Then operations succeed without mock reliance")
    void testPersistentDomainModelReadWriteAndSeedingWithoutMocks() {
        // 1. Verify Seeded Normalized Ontologies
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

        // 2. Persist new custom entries to confirm database read/write capability
        Organization customOrg = new Organization();
        customOrg.setCode("CUSTOM_REGIONAL_LAB");
        customOrg.setName("Региональная лаборатория эпидмониторинга");
        customOrg.setDescription("Центр лабораторной диагностики");
        Organization savedOrg = organizationRepository.save(customOrg);
        assertThat(savedOrg.getId()).isNotNull();

        Optional<Organization> retrievedOrg = organizationRepository.findByCode("CUSTOM_REGIONAL_LAB");
        assertThat(retrievedOrg).isPresent();
        assertThat(retrievedOrg.get().getName()).isEqualTo("Региональная лаборатория эпидмониторинга");

        // 3. Verify Dossier Aggregate Root & Document Component Mereological Composition
        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployeeId("EMP-QA-ONT-100");
        doc.setDocType("REPORT");
        doc.setTitle("Эпидемиологическое обследование очага");
        doc.setDocDate(LocalDate.of(2026, 9, 11));
        doc.setDetails("Детальный отчет по воздушно-капельному очагу");
        EmployeeDocument savedDoc = employeeDocumentRepository.save(doc);

        DossierReport aggregateRoot = new DossierReport();
        aggregateRoot.setEmployeeId("EMP-QA-ONT-100");
        aggregateRoot.setTemplateType("FULL_DOSSIER");
        aggregateRoot.setStatus("COMPLETED");
        aggregateRoot.setSummaryText("Полное эпидемиологическое досье");
        aggregateRoot.setDocumentCount(1);
        aggregateRoot.setCreatedAt(OffsetDateTime.parse("2026-09-11T10:00:00Z"));
        DossierReport savedReport = dossierReportRepository.save(aggregateRoot);

        DossierReportDocument link = new DossierReportDocument(savedReport, savedDoc);
        dossierReportDocumentRepository.save(link);

        List<DossierReportDocument> linkedDocs = dossierReportDocumentRepository.findByDossierReportId(savedReport.getId());
        assertThat(linkedDocs).hasSize(1);
        assertThat(linkedDocs.get(0).getEmployeeDocument().getId()).isEqualTo(savedDoc.getId());

        // Test aggregate root cascade behavior
        dossierReportRepository.delete(savedReport);
        dossierReportRepository.flush();

        List<DossierReportDocument> linkedDocsAfterDelete = dossierReportDocumentRepository.findByDossierReportId(savedReport.getId());
        assertThat(linkedDocsAfterDelete).isEmpty();

        // Target EmployeeDocument remains in catalog
        assertThat(employeeDocumentRepository.findById(savedDoc.getId())).isPresent();
    }

    @Test
    @DisplayName("Given bounded contexts (Document Catalog, Privacy, Strain Management), When tested end-to-end, Then boundaries remain isolated")
    @WithMockUser(username = "qa_verifier", roles = {"ADMIN"})
    void testBoundedContextsIsolationEndToEnd() throws Exception {
        // 1. Document Catalog Bounded Context
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("q", "Приказ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        // 2. Privacy Rights Bounded Context
        com.eneik.epidemiology.user.User privUser = new com.eneik.epidemiology.user.User();
        privUser.setUsername("EMP-QA-PRIV-001");
        privUser.setPasswordHash("hash");
        privUser.setRole("RESEARCHER");
        privUser.setCreatedAt(OffsetDateTime.now());
        userRepository.save(privUser);

        String exportPayload = """
                {
                    "subject_id": "EMP-QA-PRIV-001",
                    "requested_format": "JSON",
                    "notes": "Data export request under 152-FZ"
                }
                """;

        mockMvc.perform(post("/api/v1/privacy/export-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportPayload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.request_id").exists())
                .andExpect(jsonPath("$.subject_id").value("EMP-QA-PRIV-001"));

        // 3. Strain Management Bounded Context
        String strainPayload = """
                {
                    "name": "QA-Isolated-Strain-01",
                    "description": "Isolated test strain",
                    "originCountry": "Kazakhstan",
                    "severityLevel": "MEDIUM",
                    "accessDepartment": "EPIDEMIOLOGY",
                    "accessCourse": "EPI201"
                }
                """;

        mockMvc.perform(post("/api/v1/strains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strainPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("QA-Isolated-Strain-01"));

        // Verify Strain Context end-to-end retrieval
        mockMvc.perform(get("/api/v1/strains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'QA-Isolated-Strain-01')]").exists());
    }
}
