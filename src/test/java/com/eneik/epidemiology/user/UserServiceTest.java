package com.eneik.epidemiology.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import com.eneik.epidemiology.PostgresTestContainerInitializer;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainerInitializer.class)
@Transactional
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService.PasswordEncoderConfig passwordEncoderConfig;

    @Test
    @DisplayName("Given a new user is created, When saved, Then password is securely hashed and role is assigned with fixed clock")
    void testCreateUser_HashesPasswordAndAssignsRole() {
        // Inject fixed clock for deterministic timestamp assertions
        Instant fixedInstant = Instant.parse("2026-08-22T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        UserService userService = new UserService(userRepository, passwordEncoderConfig.passwordEncoder(), fixedClock);

        String rawPassword = "SecurePassword123!";
        User createdUser = userService.createUser("epidemiologist_ivan", rawPassword, "RESEARCHER");

        assertNotNull(createdUser.getId());
        assertEquals("epidemiologist_ivan", createdUser.getUsername());
        assertEquals("RESEARCHER", createdUser.getRole());
        assertNotEquals(rawPassword, createdUser.getPasswordHash());
        assertTrue(createdUser.getPasswordHash().startsWith("$2a$") || createdUser.getPasswordHash().startsWith("$2b$"));
        assertTrue(userService.verifyPassword(rawPassword, createdUser.getPasswordHash()));
        assertEquals(OffsetDateTime.ofInstant(fixedInstant, ZoneId.of("UTC")), createdUser.getCreatedAt());
    }

    @Test
    @DisplayName("Given an existing user, When schema is queried by username or ID, Then role can be quickly resolved for authorization")
    void testResolveRole_ResolvesRoleForAuthorization() {
        UserService userService = new UserService(userRepository, passwordEncoderConfig.passwordEncoder());
        User createdUser = userService.createUser("admin_anna", "AdminPass456!", "ADMIN");

        Optional<String> roleByUsername = userService.resolveRoleByUsername("admin_anna");
        assertTrue(roleByUsername.isPresent());
        assertEquals("ADMIN", roleByUsername.get());

        Optional<String> roleById = userService.resolveRoleById(createdUser.getId());
        assertTrue(roleById.isPresent());
        assertEquals("ADMIN", roleById.get());
    }

    @Test
    @DisplayName("Given existing username, When creating user with same username, Then throws IllegalArgumentException")
    void testCreateUser_DuplicateUsernameThrowsException() {
        UserService userService = new UserService(userRepository, passwordEncoderConfig.passwordEncoder());
        userService.createUser("user_unique", "Pass123!", "USER");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("user_unique", "AnotherPass!", "ADMIN");
        });
    }
}
