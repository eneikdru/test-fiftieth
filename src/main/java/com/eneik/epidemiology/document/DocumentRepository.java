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

    @Query("SELECT d FROM Document d WHERE " +
           "(:query IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:author IS NULL OR LOWER(d.authorOrganization) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:year IS NULL OR d.publicationYear = :year)")
    List<Document> searchDocuments(@Param("query") String query,
                                   @Param("author") String author,
                                   @Param("year") Integer year);
}
