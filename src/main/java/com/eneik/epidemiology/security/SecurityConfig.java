package com.eneik.epidemiology.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.OffsetDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    String json = String.format(
                        "{\"error_code\":\"UNAUTHORIZED\",\"message\":\"Требуется авторизация для выполнения данной операции.\",\"timestamp\":\"%s\"}",
                        OffsetDateTime.now()
                    );
                    response.getWriter().write(json);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    String json = String.format(
                        "{\"error_code\":\"ACCESS_DENIED\",\"message\":\"Недостаточно прав для выполнения действия. Удаление доступно только администратору.\",\"timestamp\":\"%s\"}",
                        OffsetDateTime.now()
                    );
                    response.getWriter().write(json);
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/*.html", "/*.js", "/*.css", "/*.ico", "/*.png", "/*.svg", "/static/**", "/assets/**").permitAll()
                .requestMatchers("/health", "/actuator/**", "/api/v1/auth/**", "/api/v1/recovery/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/documents/*/download", "/api/v1/documents/*/view", "/api/v1/documents/search").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/documents/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/dossier/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
