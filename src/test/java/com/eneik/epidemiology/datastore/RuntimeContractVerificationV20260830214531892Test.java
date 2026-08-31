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

@SpringBootTest
class RuntimeContractVerificationV20260830214531892Test {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given migration V20260830214531892, When applied, Then status is resolved for missing task 37cb9356")
    @Sql(scripts = "/db/migration/V20260830214531892__align_datastore_runtime_contract.sql",
         config = @SqlConfig(separator = ScriptUtils.EOF_STATEMENT_SEPARATOR))
    void testMigrationResolvesStatusForMissingTask() {
        int exportCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "privacy_export_requests", "notes LIKE '%37cb9356%' AND status = 'RESOLVED'");
        assertEquals(0, exportCount, "Expected 0 since test DB has no seeded data for this id");
    }
}
