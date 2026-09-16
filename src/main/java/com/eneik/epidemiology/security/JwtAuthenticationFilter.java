package com.eneik.epidemiology.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eneik.epidemiology.auth.TokenRevocationService;
import com.eneik.epidemiology.user.UserService;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenRevocationService tokenRevocationService, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationService = tokenRevocationService;
        this.userService = userService;
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenRevocationService tokenRevocationService) {
        this(jwtTokenProvider, tokenRevocationService, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null && !token.isEmpty()) {
                authenticateToken(token, request);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format("{\"error_code\":\"UNAUTHORIZED\",\"message\":\"Authentication failed: %s\",\"timestamp\":\"%s\"}", e.getMessage(), java.time.OffsetDateTime.now()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateToken(String token, HttpServletRequest request) {
        boolean isRevoked = tokenRevocationService.isTokenRevoked(token);
        if (isRevoked || !jwtTokenProvider.validateToken(token)) {
            throw new BadCredentialsException("Invalid or revoked JWT token");
        }

        String username = jwtTokenProvider.getUsername(token);
        if (username == null || username.trim().isEmpty()) {
            throw new BadCredentialsException("JWT token contains no valid username");
        }

        String role = resolveRole(username, token);
        if (role == null || role.trim().isEmpty()) {
            throw new BadCredentialsException("JWT token or user profile contains no valid role");
        }

        String authorityRole = role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
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
