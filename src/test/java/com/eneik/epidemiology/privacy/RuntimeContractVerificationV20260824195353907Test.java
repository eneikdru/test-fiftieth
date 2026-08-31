package com.eneik.epidemiology.privacy;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

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
import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@Transactional
class RuntimeContractVerificationV20260824195353907Test {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given mandatory Flyway migration V20260824195353907, When executed against datastore, Then patch completes cleanly and resolves pending privacy requests")
    void testRuntimeContractPatchMigrationExecutesCleanly() {
        String subjectId = "8bd0dbae-41f6-466a-95a7-aff680ed0866";
        jdbcTemplate.update(
            "INSERT INTO privacy_export_requests (request_id, subject_id, status, requested_format, created_at, notes) VALUES (?, ?, ?, ?, NOW(), ?)",
            "test-exp-907", subjectId, "PENDING", "JSON", "Initial pending export request"
        );
        jdbcTemplate.update(
            "INSERT INTO privacy_erasure_requests (request_id, subject_id, status, confirmation_token, erasure_scope, created_at, reason) VALUES (?, ?, ?, ?, ?, NOW(), ?)",
            "test-era-907", subjectId, "PROCESSING", "tok-907", "FULL", "Initial processing erasure request"
        );

        assertDoesNotThrow(() -> {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try {
                ScriptUtils.executeSqlScript(
                    conn,
                    new EncodedResource(new ClassPathResource("db/migration/V20260824195353907__align_datastore_runtime_contract.sql")),
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

        String exportStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM privacy_export_requests WHERE request_id = ?", String.class, "test-exp-907"
        );
        String erasureStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM privacy_erasure_requests WHERE request_id = ?", String.class, "test-era-907"
        );

        assertEquals("RESOLVED", exportStatus, "Pending privacy export request should be updated to RESOLVED");
        assertEquals("RESOLVED", erasureStatus, "Processing privacy erasure request should be updated to RESOLVED");
    }
}
