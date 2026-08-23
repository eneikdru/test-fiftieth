package com.eneik.data;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class MigrationTest {

    @Container
    public static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    public static void setUp() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword())
                .load();
        flyway.migrate();
    }

    @Test
    public void givenMigrationRuns_whenDatabaseSeeded_thenRealRussianSampleDocumentsInserted() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword());
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery(
                    "SELECT title, author_organization, publication_year, file_path FROM documents ORDER BY publication_year ASC");

            assertTrue(resultSet.next(), "Expected at least one document");

            // Should be the 2022 document based on sorting
            assertEquals("Методическое руководство по профилактике кори", resultSet.getString("title"));
            assertEquals("НИИ Эпидемиологии", resultSet.getString("author_organization"));
            assertEquals(2022, resultSet.getInt("publication_year"));
            assertEquals("/data/guidelines/measles_prevention.pdf", resultSet.getString("file_path"));

            assertTrue(resultSet.next(), "Expected second document");
            assertEquals("Протокол расследования вспышки сальмонеллеза", resultSet.getString("title"));
            assertEquals(2023, resultSet.getInt("publication_year"));

            assertTrue(resultSet.next(), "Expected third document");
            assertEquals("Отчет эпиднадзора по гриппу за 1 квартал", resultSet.getString("title"));
            assertEquals(2024, resultSet.getInt("publication_year"));
        }
    }

    @Test
    public void givenNewDocumentUploaded_whenSchemaPersistsData_thenFieldsAreStoredStronglyTyped() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword());
             Statement statement = connection.createStatement()) {

            // Insert a new document to test data persistence and types
            int rowsAffected = statement.executeUpdate(
                "INSERT INTO documents (title, author_organization, publication_year, file_path) " +
                "VALUES ('Тестовый документ', 'Тестовая организация', 2025, '/data/test/test.pdf')"
            );

            assertEquals(1, rowsAffected, "One row should be inserted");

            ResultSet resultSet = statement.executeQuery(
                    "SELECT id, title, author_organization, publication_year, file_path FROM documents WHERE title = 'Тестовый документ'");

            assertTrue(resultSet.next(), "Inserted document should be found");

            // Verify types by explicitly using type-specific getters
            Object id = resultSet.getObject("id");
            assertNotNull(id, "ID should be generated");
            assertEquals("java.util.UUID", id.getClass().getName(), "ID should be mapped to java.util.UUID from Postgres UUID");

            String title = resultSet.getString("title");
            assertEquals("Тестовый документ", title, "Title should be retrieved as String");

            String author = resultSet.getString("author_organization");
            assertEquals("Тестовая организация", author, "Author organization should be retrieved as String");

            int year = resultSet.getInt("publication_year");
            assertEquals(2025, year, "Publication year should be retrieved as integer");
        }
    }
}
