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
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setRole(role);
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

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Configuration
    public static class SecurityConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
