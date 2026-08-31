package com.eneik.epidemiology.privacy;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@TestPropertySource(properties = {
    "zonky.test.database.provider=zonky",
    "zonky.test.database.type=postgres"
})
@Transactional
class RuntimeContractVerificationV20260831074743161Test {

    private static final String MIGRATION_FILE_PATH = "db/migration/V20260831074743161__align_datastore_runtime_contract.sql";

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260831074743161, When executed against datastore, Then patch completes cleanly")
    void testRuntimeContractPatchMigrationExecutesCleanly() {
        assertDoesNotThrow(() -> {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try {
                ScriptUtils.executeSqlScript(
                    conn,
                    new EncodedResource(new ClassPathResource(MIGRATION_FILE_PATH)),
                    false,
                    false,
                    ScriptUtils.DEFAULT_COMMENT_PREFIX,
                    ScriptUtils.EOF_STATEMENT_SEPARATOR,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                    ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
                );
            } finally {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        });
    }
}
