package com.eneik.epidemiology.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByTitleContainingIgnoreCase(String title);
    List<Document> findByAuthorOrganizationContainingIgnoreCase(String authorOrganization);
    List<Document> findByPublicationYear(Integer year);

    @Query("SELECT d FROM Document d WHERE (:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:authorOrganization IS NULL OR LOWER(d.authorOrganization) LIKE LOWER(CONCAT('%', :authorOrganization, '%'))) " +
           "AND (:publicationYear IS NULL OR d.publicationYear = :publicationYear)")
    List<Document> searchDocuments(@Param("title") String title,
                                   @Param("authorOrganization") String authorOrganization,
                                   @Param("publicationYear") Integer publicationYear);
}
