package com.eneik.epidemiology.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByTitleContainingIgnoreCase(String title);
    List<Document> findByAuthorOrganizationContainingIgnoreCase(String authorOrganization);
    List<Document> findByPublicationYear(Integer year);

    @Query("SELECT d FROM Document d WHERE " +
           "(:query IS NULL OR LOWER(CAST(d.title AS java.lang.String)) LIKE LOWER(CONCAT('%', CAST(:query AS java.lang.String), '%'))) AND " +
           "(:author IS NULL OR LOWER(CAST(d.authorOrganization AS java.lang.String)) LIKE LOWER(CONCAT('%', CAST(:author AS java.lang.String), '%'))) AND " +
           "(:year IS NULL OR d.publicationYear = :year)")
    Page<Document> searchDocuments(@Param("query") String query,
                                   @Param("author") String author,
                                   @Param("year") Integer year,
                                   Pageable pageable);

    @Query("SELECT d FROM Document d WHERE " +
           "(:q IS NULL OR LOWER(CAST(d.title AS java.lang.String)) LIKE LOWER(CONCAT('%', CAST(:q AS java.lang.String), '%')) OR " +
           " LOWER(CAST(d.authorOrganization AS java.lang.String)) LIKE LOWER(CONCAT('%', CAST(:q AS java.lang.String), '%')) OR " +
           " (d.textContent IS NOT NULL AND LOWER(CAST(d.textContent AS java.lang.String)) LIKE LOWER(CONCAT('%', CAST(:q AS java.lang.String), '%')))) AND " +
           "(:docType IS NULL OR d.docType = :docType) AND " +
           "(:fromDate IS NULL OR d.publicationDate >= :fromDate) AND " +
           "(:toDate IS NULL OR d.publicationDate <= :toDate)")
    Page<Document> fullTextSearch(@Param("q") String q,
                                 @Param("docType") String docType,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 Pageable pageable);
}
