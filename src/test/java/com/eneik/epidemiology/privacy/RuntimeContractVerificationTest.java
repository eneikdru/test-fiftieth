package com.eneik.epidemiology.privacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Transactional
class RuntimeContractVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260823204739819, When executed against datastore, Then patch completes cleanly")
    void testRuntimeContractPatchMigrationExecutesCleanly() {
        assertDoesNotThrow(() -> {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try {
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V20260823204739819__align_datastore_runtime_contract.sql"));
            } finally {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        });
    }
}
