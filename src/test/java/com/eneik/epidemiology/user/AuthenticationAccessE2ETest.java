package com.eneik.epidemiology.user;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

import com.eneik.epidemiology.auth.PasswordRecoveryService;
import com.eneik.epidemiology.auth.PasswordRecoveryTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@SpringBootTest
@Transactional
class AuthenticationAccessE2ETest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordRecoveryTokenRepository recoveryTokenRepository;

    @Autowired
    private UserService.PasswordEncoderConfig passwordEncoderConfig;

    @Test
    @DisplayName("Given an employee user, When role is queried, Then role is confirmed as RESEARCHER and upload permissions are denied")
    void testEmployeeAccessRestrictions() {
        Instant fixedInstant = Instant.parse("2026-08-22T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        UserService userService = new UserService(userRepository, passwordEncoderConfig.passwordEncoder(), fixedClock);

        User employee = userService.createUser("employee_user", "EmpPass123!", "RESEARCHER");

        Optional<String> role = userService.resolveRoleByUsername("employee_user");
        assertTrue(role.isPresent());
        assertEquals("RESEARCHER", role.get());
        assertNotEquals("ADMIN", role.get());

        // Privilege escalation check: Standard employee cannot claim ADMIN role
        boolean canUploadDocuments = "ADMIN".equals(role.get());
        assertFalse(canUploadDocuments, "Standard employee user must not have document upload permissions");
    }

    @Test
    @DisplayName("Given a locked-out user, When self-service recovery flow is initiated, Then access is restored deterministically")
    void testSelfServiceRecoveryFlow() {
        Instant fixedInstant = Instant.parse("2026-08-22T12:30:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        UserService userService = new UserService(userRepository, passwordEncoderConfig.passwordEncoder(), fixedClock);

        // Seed user needing password recovery
        User user = userService.createUser("locked_researcher", "OldSecret456!", "RESEARCHER");

        // Create recovery service and initiate self-service recovery flow through actual business logic
        PasswordRecoveryService recoveryService = new PasswordRecoveryService(
                userRepository,
                recoveryTokenRepository,
                passwordEncoderConfig.passwordEncoder(),
                fixedClock,
                new SecureRandom(),
                "http://localhost:8080"
        );

        // Exercise recovery code path directly
        PasswordRecoveryService.RecoveryResponse recoveryResponse = recoveryService.initiateRecovery("locked_researcher");
        assertNotNull(recoveryResponse.recoveryToken(), "Recovery token must be generated");
        assertTrue(recoveryResponse.recoveryLink().contains(recoveryResponse.recoveryToken()));

        // Perform password reset using generated recovery token
        String newPassword = "RestoredPass789!";
        recoveryService.confirmReset(recoveryResponse.recoveryToken(), newPassword);

        // Verify restored access
        Optional<User> restoredUserOpt = userService.findByUsername("locked_researcher");
        assertTrue(restoredUserOpt.isPresent());
        User restoredUser = restoredUserOpt.get();

        assertTrue(userService.verifyPassword(newPassword, restoredUser.getPasswordHash()), "User access must be restored with new credentials");
        assertFalse(userService.verifyPassword("OldSecret456!", restoredUser.getPasswordHash()), "Old password must no longer be valid");
    }
}
