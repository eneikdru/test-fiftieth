package com.eneik.epidemiology.datastore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RuntimeContractVerificationV20260831022904620Test {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given migration V20260831022904620, When executed, Then database context and migration script execute cleanly")
    @Sql(scripts = "/db/migration/V20260831022904620__align_datastore_runtime_contract.sql",
         config = @SqlConfig(separator = ScriptUtils.EOF_STATEMENT_SEPARATOR))
    void testMigrationExecutesCleanly() {
        assertNotNull(dataSource, "DataSource must be available");
        assertNotNull(jdbcTemplate, "JdbcTemplate must be available");
        int exportCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "privacy_export_requests", "notes LIKE '%0386e9cd%' AND status = 'RESOLVED'");
        assertEquals(0, exportCount, "Expected 0 since test DB has no seeded data for unapplied migration 0386e9cd");
    }
}
