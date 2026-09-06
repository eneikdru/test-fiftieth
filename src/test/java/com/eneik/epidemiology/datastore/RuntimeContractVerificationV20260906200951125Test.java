package com.eneik.epidemiology.datastore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
public class RuntimeContractVerificationV20260906200951125Test {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Given local database with applied missing migration 20260904202315126, When Flyway repair and migrate execute, Then validation succeeds without error")
    void testFlywayValidationPassesWithMissingAppliedMigration() {
        assertDoesNotThrow(() -> {
            flyway.repair();
            flyway.migrate();
        });
    }
}
