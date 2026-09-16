package com.eneik.epidemiology.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.user.UserService;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;
    private final UserService userService;
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenRevocationService tokenRevocationService, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationService = tokenRevocationService;
        this.userService = userService;
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenRevocationService tokenRevocationService) {
        this(jwtTokenProvider, tokenRevocationService, null);
    }

    @Autowired(required = false)
    public void setAuthenticationEntryPoint(@Lazy AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && !token.isEmpty()) {
            try {
                processTokenAuthentication(token, request);
            } catch (Exception e) {
                AuthenticationException authException = e instanceof AuthenticationException ?
                        (AuthenticationException) e :
                        new BadCredentialsException("Token validation failed: " + e.getMessage(), e);

                handleAuthenticationFailure(request, response, authException);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        if (authenticationEntryPoint != null) {
            authenticationEntryPoint.commence(request, response, authException);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String json = String.format(
                    "{\"error_code\":\"UNAUTHORIZED\",\"message\":\"Требуется авторизация для выполнения данной операции.\",\"timestamp\":\"%s\"}",
                    OffsetDateTime.now()
            );
            response.getWriter().write(json);
        }
    }

    private void processTokenAuthentication(String token, HttpServletRequest request) {
        if (tokenRevocationService.isTokenRevoked(token)) {
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            return;
        }

        String username = jwtTokenProvider.getUsername(token);
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        String role = resolveRole(username, token);
        if (role == null || role.trim().isEmpty()) {
            return;
        }

        role = role.trim().toUpperCase();
        String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityRole);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, Collections.singletonList(authority));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveRole(String username, String token) {
        if (userService != null) {
            Optional<String> persistentRole = userService.resolveRoleByUsername(username);
            if (persistentRole.isPresent() && !persistentRole.get().trim().isEmpty()) {
                return persistentRole.get();
            }
        }
        return jwtTokenProvider.getRole(token);
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String trimmedHeader = authHeader.trim();
            if (trimmedHeader.toLowerCase().startsWith("bearer ")) {
                return trimmedHeader.substring(7).trim();
            }
        }
        String paramToken = request.getParameter("access_token");
        if (paramToken == null || paramToken.trim().isEmpty()) {
            paramToken = request.getParameter("token");
        }
        if (paramToken != null && !paramToken.trim().isEmpty()) {
            return paramToken.trim();
        }
        return null;
    }
}
