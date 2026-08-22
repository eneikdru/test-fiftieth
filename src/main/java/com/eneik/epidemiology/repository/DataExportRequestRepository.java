package com.eneik.epidemiology.repository;

import com.eneik.epidemiology.domain.DataExportRequestEntity;
import com.eneik.epidemiology.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface DataExportRequestRepository extends JpaRepository<DataExportRequestEntity, UUID> {

    Optional<DataExportRequestEntity> findBySubjectIdAndStatusIn(String subjectId, Collection<JobStatus> statuses);

    @Modifying
    @Query("UPDATE DataExportRequestEntity e SET e.status = :newStatus, e.downloadUrl = :downloadUrl, e.completedAt = :completedAt, e.expiresAt = :expiresAt WHERE e.requestId = :requestId AND e.status = :expectedStatus")
    int transitionToCompleted(
            @Param("requestId") UUID requestId,
            @Param("expectedStatus") JobStatus expectedStatus,
            @Param("newStatus") JobStatus newStatus,
            @Param("downloadUrl") String downloadUrl,
            @Param("completedAt") OffsetDateTime completedAt,
            @Param("expiresAt") OffsetDateTime expiresAt
    );

    @Modifying
    @Query("UPDATE DataExportRequestEntity e SET e.status = :newStatus, e.errorCode = :errorCode, e.errorMessage = :errorMessage, e.completedAt = :completedAt WHERE e.requestId = :requestId AND e.status = :expectedStatus")
    int transitionToFailed(
            @Param("requestId") UUID requestId,
            @Param("expectedStatus") JobStatus expectedStatus,
            @Param("newStatus") JobStatus newStatus,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("completedAt") OffsetDateTime completedAt
    );
}
