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

    @Query(value = "SELECT d.id AS id, " +
           "d.title AS title, " +
           "d.doc_type AS docType, " +
           "d.author_organization AS authorOrganization, " +
           "d.publication_year AS publicationYear, " +
           "d.publication_date AS publicationDate, " +
           "d.file_path AS filePath, " +
           "d.text_content AS textContent, " +
           "d.created_at AS createdAt, " +
           "CASE " +
           "  WHEN :q IS NOT NULL AND length(trim(:q)) > 0 THEN " +
           "    GREATEST(0.01, ts_rank(" +
           "      to_tsvector('russian', coalesce(d.title, '') || ' ' || coalesce(d.author_organization, '') || ' ' || coalesce(d.text_content, '')), " +
           "      plainto_tsquery('russian', :q)" +
           "    )) " +
           "  ELSE 1.0 " +
           "END AS relevanceScore " +
           "FROM documents d WHERE " +
           "(:q IS NULL OR d.title ILIKE concat('%', :q, '%') OR d.author_organization ILIKE concat('%', :q, '%') OR d.text_content ILIKE concat('%', :q, '%')) AND " +
           "(:docType IS NULL OR d.doc_type = :docType) AND " +
           "(CAST(:fromDate AS date) IS NULL OR d.publication_date >= CAST(:fromDate AS date)) AND " +
           "(CAST(:toDate AS date) IS NULL OR d.publication_date <= CAST(:toDate AS date)) " +
           "ORDER BY relevanceScore DESC, d.id ASC",
           countQuery = "SELECT count(*) FROM documents d WHERE " +
           "(:q IS NULL OR d.title ILIKE concat('%', :q, '%') OR d.author_organization ILIKE concat('%', :q, '%') OR d.text_content ILIKE concat('%', :q, '%')) AND " +
           "(:docType IS NULL OR d.doc_type = :docType) AND " +
           "(CAST(:fromDate AS date) IS NULL OR d.publication_date >= CAST(:fromDate AS date)) AND " +
           "(CAST(:toDate AS date) IS NULL OR d.publication_date <= CAST(:toDate AS date))",
           nativeQuery = true)
    Page<DocumentSearchResultProjection> fullTextSearch(@Param("q") String q,
                                                       @Param("docType") String docType,
                                                       @Param("fromDate") LocalDate fromDate,
                                                       @Param("toDate") LocalDate toDate,
                                                       Pageable pageable);
}
