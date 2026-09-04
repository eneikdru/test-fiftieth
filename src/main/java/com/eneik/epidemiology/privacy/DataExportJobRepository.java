package com.eneik.epidemiology.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface DataExportJobRepository extends JpaRepository<DataExportJob, String> {

    List<DataExportJob> findBySubjectIdAndStatusIn(String subjectId, List<String> statuses);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DataExportJob j SET j.status = :newStatus, j.downloadUrl = :downloadUrl, j.exportPayload = :exportPayload, j.completedAt = :completedAt, j.expiresAt = :expiresAt WHERE j.requestId = :requestId AND j.status = :expectedStatus")
    int updateStatusToCompleted(
        @Param("requestId") String requestId,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus,
        @Param("downloadUrl") String downloadUrl,
        @Param("exportPayload") String exportPayload,
        @Param("completedAt") OffsetDateTime completedAt,
        @Param("expiresAt") OffsetDateTime expiresAt
    );
}
