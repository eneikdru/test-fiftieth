package com.eneik.epidemiology.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface DataErasureJobRepository extends JpaRepository<DataErasureJob, String> {

    List<DataErasureJob> findBySubjectIdAndStatusIn(String subjectId, List<String> statuses);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DataErasureJob j SET j.status = :newStatus, j.recordsErasedCount = :recordsErasedCount, j.completedAt = :completedAt WHERE j.requestId = :requestId AND j.status = :expectedStatus")
    int updateStatusToCompleted(
        @Param("requestId") String requestId,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus,
        @Param("recordsErasedCount") Integer recordsErasedCount,
        @Param("completedAt") OffsetDateTime completedAt
    );
}
