package com.eneik.epidemiology.process;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface BackgroundProcessRepository extends JpaRepository<BackgroundProcess, UUID> {

    @Modifying
    @Query("UPDATE BackgroundProcess r SET r.status = :newStatus, r.updatedAt = :now WHERE r.id = :id AND r.status = :expectedStatus")
    int updateStatusAtomically(
        @Param("id") UUID id,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus,
        @Param("now") OffsetDateTime now
    );
}
