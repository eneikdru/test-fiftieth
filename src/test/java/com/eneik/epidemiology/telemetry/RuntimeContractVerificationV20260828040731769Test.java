package com.eneik.epidemiology.telemetry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class RuntimeContractVerificationV20260828040731769Test {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260828040731769, When executed against datastore, Then patch completes cleanly and columns exist")
    void testRuntimeContractPatchMigrationExecutesCleanly() {
        assertDoesNotThrow(() -> {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try {
                ScriptUtils.executeSqlScript(
                    conn,
                    new EncodedResource(new ClassPathResource("db/migration/V20260828040731769__add_workflow_duration_telemetry.sql")),
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

        Boolean columnExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) > 0 FROM information_schema.columns WHERE table_name = 'telemetry_events' AND column_name = 'workflow_duration_ms'",
            Boolean.class
        );
        assertTrue(Boolean.TRUE.equals(columnExists), "Column workflow_duration_ms should exist in telemetry_events table");
    }
}
