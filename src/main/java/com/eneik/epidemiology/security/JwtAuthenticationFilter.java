package com.eneik.epidemiology.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
        String token = resolveToken(request);

        if (token != null && !token.isEmpty()) {
            if (jwtTokenProvider.validateToken(token) && !tokenRevocationService.isTokenRevoked(token)) {
                String username = jwtTokenProvider.getUsername(token);

                boolean userVerifiedInPersistence = true;
                String role = null;
                if (username != null && !username.trim().isEmpty() && userService != null) {
                    Optional<String> persistentRole = userService.resolveRoleByUsername(username);
                    if (persistentRole.isPresent()) {
                        if (!persistentRole.get().trim().isEmpty()) {
                            role = persistentRole.get();
                        }
                    } else {
                        userVerifiedInPersistence = false;
                    }
                }

                if (userVerifiedInPersistence) {
                    if (role == null || role.trim().isEmpty()) {
                        role = jwtTokenProvider.getRole(token);
                    }

                    if (role == null || role.trim().isEmpty()) {
                        role = "USER";
                    }
                    role = role.trim().toUpperCase();

                    String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityRole);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
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
