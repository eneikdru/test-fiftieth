package com.eneik.epidemiology.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    @DisplayName("Given a new document is uploaded, When the schema persists the data, Then the title, author-organization, and year are stored strongly typed.")
    void testSaveDocument_StronglyTypedMetadataPersisted() {
        // Given
        Document newDoc = new Document("Новое исследование", "Медицинский университет", 2024, "/data/docs/2024/new_study.pdf");

        // When
        Document savedDoc = documentRepository.saveAndFlush(newDoc);

        // Then
        assertThat(savedDoc.getId()).isNotNull();
        assertThat(savedDoc.getTitle()).isEqualTo("Новое исследование");
        assertThat(savedDoc.getAuthorOrganization()).isEqualTo("Медицинский университет");
        assertThat(savedDoc.getPublicationYear()).isEqualTo(2024);
        assertThat(savedDoc.getFilePath()).isEqualTo("/data/docs/2024/new_study.pdf");
        assertThat(savedDoc.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Given a document with extracted text, When it is saved, Then the text content is correctly persisted.")
    void testSaveDocument_TextContentIsPersisted() {
        // Given
        Document doc = new Document("Test Text Extraction", "Lab", 2024, "/path/doc.pdf");
        doc.setTextContent("Извлеченный текст для тестирования полнотекстового поиска.");

        // When
        Document savedDoc = documentRepository.saveAndFlush(doc);

        // Then
        assertThat(savedDoc.getTextContent()).isEqualTo("Извлеченный текст для тестирования полнотекстового поиска.");
    }

    @Test
    @DisplayName("Given an initial deployment, When the database is seeded, Then real Russian epidemiological sample documents are inserted.")
    void testInitialDeployment_DatabaseSeededWithRussianDocuments() {
        // Verify seed data from Flyway migration V20260822225105263__create_documents_table.sql

        List<Document> salmonellaDocs = documentRepository.findByTitleContainingIgnoreCase("сальмонеллеза");
        assertThat(salmonellaDocs).isNotEmpty();
        Document salmonellaDoc = salmonellaDocs.get(0);
        assertThat(salmonellaDoc.getAuthorOrganization()).isEqualTo("НИИ Эпидемиологии");
        assertThat(salmonellaDoc.getPublicationYear()).isEqualTo(2023);

        List<Document> fluDocs = documentRepository.findByTitleContainingIgnoreCase("гриппу");
        assertThat(fluDocs).isNotEmpty();

        List<Document> measlesDocs = documentRepository.findByTitleContainingIgnoreCase("кори");
        assertThat(measlesDocs).isNotEmpty();

        long totalCount = documentRepository.count();
        assertThat(totalCount).isGreaterThanOrEqualTo(3);
    }
}
