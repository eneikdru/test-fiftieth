package com.eneik.epidemiology.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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
}
