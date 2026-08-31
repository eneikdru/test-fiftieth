package com.eneik.epidemiology.categorization;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@Transactional
class Epic10ReviewConcernsCategorizationMigrationTest {

    @Autowired
    private DesignReviewConcernRepository concernRepository;

    @Autowired
    private RootCausePatternRepository patternRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260823170105641, When executed against datastore, Then seeds epic sequence 10 concern and categorizes reviewConcerns stream with RCP-REVIEW-CONCERNS-001")
    void testEpic10CategorizationMigrationExecutesCleanly() {
        try {
            ClassPathResource resource = new ClassPathResource("db/migration/V20260823170105641__categorize_epic_10_design_review_concerns.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            fail("Migration V20260823170105641 execution failed: " + e.getMessage());
        }

        entityManager.clear();

        RootCausePattern pattern = patternRepository.findByStreamName("reviewConcerns").orElseThrow();
        assertEquals("RCP-REVIEW-CONCERNS-001", pattern.getInvariantPatternId());

        DesignReviewConcern epic10Concern = concernRepository.findById("DRC-EPIC-10").orElseThrow();
        assertEquals("reviewConcerns", epic10Concern.getStreamName());
        assertEquals(10, epic10Concern.getEpicSequence());
        assertEquals("RCP-REVIEW-CONCERNS-001", epic10Concern.getRootCausePatternId());
        assertEquals("CATEGORIZED", epic10Concern.getStatus());
    }
}
