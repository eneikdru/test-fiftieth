package com.eneik.epidemiology.datastore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class RuntimeContractVerificationV20260903062419050Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given Flyway migration V20260903062419050, When applied, Then datastore schema complies with runtime contract")
    void verifyRuntimeContractAlignment() {
        Boolean exportTableExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'privacy_export_requests')",
                Boolean.class
        );
        assertTrue(exportTableExists != null && exportTableExists, "Table 'privacy_export_requests' must exist in the database schema");

        Boolean erasureTableExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'privacy_erasure_requests')",
                Boolean.class
        );
        assertTrue(erasureTableExists != null && erasureTableExists, "Table 'privacy_erasure_requests' must exist in the database schema");
    }
}
