package com.eneik.production.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, Long> {
    List<TelemetryEvent> findByEventType(TelemetryEventType eventType);
}
