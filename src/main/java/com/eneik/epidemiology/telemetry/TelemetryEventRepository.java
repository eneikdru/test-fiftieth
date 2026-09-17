package com.eneik.epidemiology.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, Long> {

    List<TelemetryEvent> findByEventType(String eventType);

    List<TelemetryEvent> findByDocumentId(Long documentId);

    List<TelemetryEvent> findByQueryTerm(String queryTerm);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TelemetryEvent t WHERE t.queryTerm = :subjectId")
    int deleteByQueryTerm(@Param("subjectId") String subjectId);
}
