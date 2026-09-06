package com.eneik.epidemiology.datastore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FlywayMigrationPatternConfigurationTest {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Given Flyway configuration, When inspected, Then ignoreMigrationPatterns includes missing migration pattern")
    void testFlywayIgnoreMigrationPatternsConfigured() {
        boolean matchesMissing = Arrays.stream(flyway.getConfiguration().getIgnoreMigrationPatterns())
                .anyMatch(pattern -> pattern != null && pattern.toString().contains("missing"));
        assertTrue(matchesMissing, "Flyway ignoreMigrationPatterns should include missing migration pattern");
    }
}
