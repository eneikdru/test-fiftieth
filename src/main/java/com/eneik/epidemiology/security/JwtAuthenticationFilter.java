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
        try {
            String token = resolveToken(request);

            if (token != null && !token.isEmpty()) {
                boolean isRevoked = false;
                try {
                    isRevoked = tokenRevocationService.isTokenRevoked(token);
                } catch (Exception e) {
                    logger.debug("Error checking token revocation status: " + e.getMessage(), e);
                }

                if (!isRevoked && jwtTokenProvider.validateToken(token)) {
                    String username = null;
                    try {
                        username = jwtTokenProvider.getUsername(token);
                    } catch (Exception e) {
                        logger.debug("Error extracting username from token: " + e.getMessage(), e);
                    }

                    if (username != null && !username.trim().isEmpty()) {
                        String role = null;
                        if (userService != null) {
                            try {
                                Optional<String> persistentRole = userService.resolveRoleByUsername(username);
                                if (persistentRole.isPresent() && !persistentRole.get().trim().isEmpty()) {
                                    role = persistentRole.get();
                                }
                            } catch (Exception e) {
                                logger.debug("Could not resolve persistent role for user during JWT filter processing: " + username, e);
                            }
                        }

                        if (role == null || role.trim().isEmpty()) {
                            try {
                                role = jwtTokenProvider.getRole(token);
                            } catch (Exception e) {
                                logger.debug("Could not extract role from JWT token: " + e.getMessage(), e);
                            }
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
        } catch (Exception e) {
            logger.warn("Unexpected exception encountered during JWT authentication processing: " + e.getMessage(), e);
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
