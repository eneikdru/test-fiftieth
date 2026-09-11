package com.eneik.epidemiology.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should retrieve standard normalized roles seeded by Flyway migration")
    void testStandardRolesExist() {
        List<String> expectedRoles = List.of("ADMIN", "EPIDEMIOLOGIST", "RESEARCHER", "USER");

        for (String roleName : expectedRoles) {
            Optional<Role> roleOpt = roleRepository.findByName(roleName);
            assertThat(roleOpt).isPresent();
            assertThat(roleOpt.get().getName()).isEqualTo(roleName);
            assertThat(roleOpt.get().getDescription()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should persist and retrieve custom normalized role")
    void testSaveAndFindRole() {
        Role customRole = new Role("AUDITOR", "Аудитор информационной безопасности");
        Role saved = roleRepository.save(customRole);

        assertThat(saved.getId()).isNotNull();

        Optional<Role> fetched = roleRepository.findByName("AUDITOR");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("AUDITOR");
        assertThat(fetched.get().getDescription()).isEqualTo("Аудитор информационной безопасности");
    }

    @Test
    @DisplayName("Should verify user roles junction table mapping from structured database layer")
    void testUserRoleJunctionTable() {
        User user = new User("normalized_test_user", "hashed_pass", "EPIDEMIOLOGIST", "test_norm@example.com", "Иванов Иван");
        User savedUser = userRepository.save(user);

        Role role = roleRepository.findByName("EPIDEMIOLOGIST").orElseThrow();

        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                savedUser.getId(), role.getId());

        List<String> rolesForUser = jdbcTemplate.queryForList(
                "SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = ?",
                String.class,
                savedUser.getId()
        );

        assertThat(rolesForUser).contains("EPIDEMIOLOGIST");
    }
}
