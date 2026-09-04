package com.eneik.epidemiology.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class MoodleRoleMappingDataVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Given role mapping tables, When queried, Then Moodle roles map to internal domain roles.")
    void testMoodleRoleMappings() {
        List<String> adminRoles = jdbcTemplate.queryForList("SELECT internal_role FROM moodle_role_mappings WHERE moodle_role_pattern = 'администратор'", String.class);
        assertEquals(1, adminRoles.size());
        assertEquals("ADMIN", adminRoles.get(0));

        List<String> epiRoles = jdbcTemplate.queryForList("SELECT internal_role FROM moodle_role_mappings WHERE moodle_role_pattern = 'старший научный сотрудник'", String.class);
        assertEquals(1, epiRoles.size());
        assertEquals("EPIDEMIOLOGIST", epiRoles.get(0));

        List<String> researcherRoles = jdbcTemplate.queryForList("SELECT internal_role FROM moodle_role_mappings WHERE moodle_role_pattern = 'исследователь'", String.class);
        assertEquals(1, researcherRoles.size());
        assertEquals("RESEARCHER", researcherRoles.get(0));
    }
}
