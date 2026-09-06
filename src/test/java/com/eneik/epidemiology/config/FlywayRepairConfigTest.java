package com.eneik.epidemiology.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class FlywayRepairConfigTest {

    @Test
    @DisplayName("Given FlywayRepairConfig, When flywayMigrationStrategy is invoked, Then repair is executed before migrate")
    void testFlywayMigrationStrategyExecutesRepairBeforeMigrate() {
        FlywayRepairConfig config = new FlywayRepairConfig();
        FlywayMigrationStrategy strategy = config.flywayMigrationStrategy();
        assertNotNull(strategy, "FlywayMigrationStrategy should not be null");

        Flyway mockFlyway = mock(Flyway.class);
        strategy.migrate(mockFlyway);

        var inOrder = inOrder(mockFlyway);
        inOrder.verify(mockFlyway).repair();
        inOrder.verify(mockFlyway).migrate();
    }
}
