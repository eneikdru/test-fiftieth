package com.eneik.epidemiology.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class FlywayConfigurationTest {

    @Test
    void flywayMigrationStrategy_RepairsBeforeMigrating() {
        FlywayConfiguration config = new FlywayConfiguration();
        FlywayMigrationStrategy strategy = config.flywayMigrationStrategy();

        Flyway flyway = mock(Flyway.class);
        strategy.migrate(flyway);

        InOrder inOrder = inOrder(flyway);
        inOrder.verify(flyway).repair();
        inOrder.verify(flyway).migrate();
    }
}
