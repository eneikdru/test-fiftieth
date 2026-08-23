package com.eneik.epidemiology.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, Long> {

    List<TelemetryEvent> findByEventType(String eventType);

    List<TelemetryEvent> findByDocumentId(Long documentId);
}
