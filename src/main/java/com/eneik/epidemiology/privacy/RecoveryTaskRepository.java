package com.eneik.epidemiology.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RecoveryTaskRepository extends JpaRepository<RecoveryTask, UUID> {

    @Modifying
    @Query("UPDATE RecoveryTask r SET r.status = :newStatus, r.updatedAt = :now WHERE r.id = :id AND r.status = :expectedStatus")
    int updateStatusAtomically(
        @Param("id") UUID id,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus,
        @Param("now") OffsetDateTime now
    );
}
