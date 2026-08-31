package com.eneik.epidemiology.datastore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RuntimeContractVerificationV20260831041547306Test {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Given Flyway configuration, When inspected, Then outOfOrder execution is enabled")
    void testFlywayOutOfOrderExecutionIsEnabled() {
        assertTrue(flyway.getConfiguration().isOutOfOrder(), "Flyway outOfOrder should be enabled");
    }
}
