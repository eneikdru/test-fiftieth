package com.eneik.epidemiology.document;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@Transactional
class EmployeeDocumentRepositoryTest {

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Given Flyway migration V20260824101828187, When executed against datastore, Then employee_documents schema and indexes execute cleanly")
    void testMigrationExecutesCleanly() {
        assertDoesNotThrow(() -> {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try {
                ScriptUtils.executeSqlScript(
                    conn,
                    new EncodedResource(new ClassPathResource("db/migration/V20260824101828187__employee_document_aggregation.sql")),
                    false,
                    false,
                    ScriptUtils.DEFAULT_COMMENT_PREFIX,
                    ScriptUtils.EOF_STATEMENT_SEPARATOR,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
                );
            } finally {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        });
    }

    @Test
    @DisplayName("Given an employee ID, when querying for related documents, then the system returns a unified set of orders, reports, and exams.")
    void testFindUnifiedEmployeeDossier_ReturnsUnifiedDocumentSet() {
        // Given
        String employeeId = "EMP-TEST-100";
        EmployeeDocument orderDoc = new EmployeeDocument(employeeId, "ORDER", "Приказ о назначении", LocalDate.of(2023, 1, 10), "Приказ №10");
        EmployeeDocument reportDoc = new EmployeeDocument(employeeId, "REPORT", "Отчет по теме 1", LocalDate.of(2023, 5, 15), "Годовой отчет");
        EmployeeDocument examDoc = new EmployeeDocument(employeeId, "EXAM", "Экзаменационная ведомость", LocalDate.of(2023, 9, 20), "Сдан успешно");
        EmployeeDocument extractDoc = new EmployeeDocument(employeeId, "EXTRACT", "Выписка из протокола", LocalDate.of(2023, 11, 1), "Ученый совет");

        employeeDocumentRepository.saveAll(List.of(orderDoc, reportDoc, examDoc, extractDoc));

        // When
        List<EmployeeDocument> dossier = employeeDocumentRepository.findUnifiedEmployeeDossier(employeeId);

        // Then
        assertThat(dossier).hasSize(4);
        Set<String> docTypes = dossier.stream().map(EmployeeDocument::getDocType).collect(Collectors.toSet());
        assertThat(docTypes).containsExactlyInAnyOrder("ORDER", "REPORT", "EXAM", "EXTRACT");
    }

    @Test
    @DisplayName("Given a missing employee, when querying, then it returns an empty set.")
    void testFindUnifiedEmployeeDossier_MissingEmployee_ReturnsEmptySet() {
        // Given
        String missingEmployeeId = "NON-EXISTENT-EMP-999999";

        // When
        List<EmployeeDocument> dossier = employeeDocumentRepository.findUnifiedEmployeeDossier(missingEmployeeId);

        // Then
        assertThat(dossier).isEmpty();
    }

    @Test
    @DisplayName("Given employee document records, when saved and queried, then strongly typed fields and sorting by date are preserved.")
    void testPersistenceAndSorting() {
        // Given
        String employeeId = "EMP-TEST-200";
        EmployeeDocument oldDoc = new EmployeeDocument(employeeId, "ORDER", "Старый приказ", LocalDate.of(2020, 1, 1), "Детали 1");
        EmployeeDocument newDoc = new EmployeeDocument(employeeId, "ORDER", "Новый приказ", LocalDate.of(2024, 1, 1), "Детали 2");

        employeeDocumentRepository.save(oldDoc);
        employeeDocumentRepository.save(newDoc);

        // When
        List<EmployeeDocument> results = employeeDocumentRepository.findByEmployeeIdOrderByDocDateDesc(employeeId);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("Новый приказ");
        assertThat(results.get(1).getTitle()).isEqualTo("Старый приказ");
    }

    @Test
    @DisplayName("Given employee documents with scientific directions, when queried by scientific direction, then it filters correctly.")
    void testSearchEmployeeDocuments_ByScientificDirection() {
        // Given
        String employeeId = "EMP-TEST-300";
        EmployeeDocument virologyDoc = new EmployeeDocument(employeeId, "REPORT", "Отчет по вирусологии", LocalDate.of(2023, 5, 10), "Детали", "Вирусология");
        EmployeeDocument bacteriologyDoc = new EmployeeDocument(employeeId, "PUBLICATION", "Статья по бактериологии", LocalDate.of(2023, 6, 15), "Детали", "Бактериология");

        employeeDocumentRepository.save(virologyDoc);
        employeeDocumentRepository.save(bacteriologyDoc);

        // When
        List<EmployeeDocument> virologyResults = employeeDocumentRepository.searchEmployeeDocuments(
                employeeId, null, null, "Вирусология", null, null, null
        );

        // Then
        assertThat(virologyResults).hasSize(1);
        assertThat(virologyResults.get(0).getScientificDirection()).isEqualTo("Вирусология");
    }
}
