package com.eneik.epidemiology.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, Clock.systemUTC());
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public User createUser(String username, String rawPassword, String role) {
        return createUser(username, rawPassword, null, null, role);
    }

    @Transactional
    public User createUser(String username, String rawPassword, String email, String fullName, String role) {
        return createUserWithMoodle(username, rawPassword, email, fullName, role, null, null);
    }

    @Transactional
    public User createUserWithMoodle(String username, String rawPassword, String email, String fullName, String role, String moodleId, String department) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        if (email != null && !email.trim().isEmpty() && userRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setRole(role != null ? role : "USER");
        user.setEmail(email != null ? email.trim() : null);
        user.setFullName(fullName != null ? fullName.trim() : null);
        user.setMoodleId(moodleId);
        user.setDepartment(department);
        user.setCreatedAt(OffsetDateTime.now(clock));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveRoleByUsername(String username) {
        return userRepository.findRoleByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveRoleById(Long id) {
        return userRepository.findRoleById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            return Optional.empty();
        }
        String trimmed = identity.trim();
        return userRepository.findByUsernameOrEmail(trimmed, trimmed);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByMoodleId(String moodleId) {
        return userRepository.findByMoodleId(moodleId);
    }

    @Transactional
    public int updateRoleAtomically(Long id, String oldRole, String newRole) {
        return userRepository.updateRoleAtomically(id, oldRole, newRole);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Configuration
    public static class PasswordEncoderConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
