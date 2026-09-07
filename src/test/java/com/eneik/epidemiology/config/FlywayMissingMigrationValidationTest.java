package com.eneik.epidemiology.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class FlywayMissingMigrationValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given applied migration 20260904202315126, When application starts, Then Flyway validation passes and moodle_role_mappings exist")
    void testFlywayValidationWithMissingMigrationResolved() {
        assertNotNull(jdbcTemplate, "JdbcTemplate must be injected when Spring context initializes successfully");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM moodle_role_mappings", Integer.class);
        assertNotNull(count, "Count of moodle_role_mappings must not be null");

        List<String> adminRoles = jdbcTemplate.queryForList(
                "SELECT internal_role FROM moodle_role_mappings WHERE moodle_role_pattern = 'администратор'",
                String.class
        );
        assertEquals(1, adminRoles.size());
        assertEquals("ADMIN", adminRoles.get(0));
    }
}
