package com.eneik.epidemiology.privacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Transactional
class RuntimeContractVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260824021332426, When executed against datastore, Then patch completes cleanly")
    void testRuntimeContractPatchMigrationExecutesCleanly() {
        assertDoesNotThrow(() -> {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V20260824021332426__align_datastore_runtime_contract.sql")
            );
            populator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
            populator.execute(dataSource);
        });
    }
}
