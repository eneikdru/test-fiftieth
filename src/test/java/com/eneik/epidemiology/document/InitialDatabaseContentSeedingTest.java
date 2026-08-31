package com.eneik.epidemiology.document;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@Transactional
public class InitialDatabaseContentSeedingTest {

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DossierReportRepository dossierReportRepository;

    @Test
    @DisplayName("Given initial database seeding runs, Then isolated strain records and dossier reports exist with valid non-lorem content")
    void testInitialDatabaseContentSeededWithoutLoremIpsum() {
        List<EmployeeDocument> documents = employeeDocumentRepository.findAll();
        assertFalse(documents.isEmpty(), "Employee documents table should contain seeded records");

        List<EmployeeDocument> strainDocs = documents.stream()
                .filter(doc -> "STRAIN_ISOLATION".equals(doc.getDocType()))
                .toList();
        assertFalse(strainDocs.isEmpty(), "Should contain isolated strain documents");

        List<DossierReport> reports = dossierReportRepository.findAll();
        assertFalse(reports.isEmpty(), "Dossier reports table should contain seeded records");

        // Verify no lorem ipsum text exists in titles, details, or summary text
        for (EmployeeDocument doc : documents) {
            if (doc.getTitle() != null) {
                assertFalse(doc.getTitle().toLowerCase().contains("lorem ipsum"), "Title should not contain placeholder text");
            }
            if (doc.getDetails() != null) {
                assertFalse(doc.getDetails().toLowerCase().contains("lorem ipsum"), "Details should not contain placeholder text");
            }
        }

        for (DossierReport report : reports) {
            if (report.getSummaryText() != null) {
                assertFalse(report.getSummaryText().toLowerCase().contains("lorem ipsum"), "Summary text should not contain placeholder text");
            }
        }
    }
}
