package com.eneik.epidemiology.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DataErasureTokenRepository extends JpaRepository<DataErasureToken, Long> {
    Optional<DataErasureToken> findByToken(String token);

    void deleteBySubjectId(String subjectId);

    @Modifying
    @Query("UPDATE DataErasureToken t SET t.used = true WHERE t.id = :id AND t.used = false")
    int markTokenAsUsed(@Param("id") Long id);
}