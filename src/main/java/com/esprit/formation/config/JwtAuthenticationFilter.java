package com.esprit.formation.config;

import com.esprit.formation.entities.RevokedToken;
import com.esprit.formation.services.CustomUserDetailsService;
import com.esprit.formation.services.JwtService;
import com.esprit.formation.services.UserService;
import com.esprit.formation.utils.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService, UserService userService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        LOGGER.info("Processing request in JwtAuthenticationFilter...");

        // Skip JWT processing for /api/auth/login
        if (path.endsWith("/api/auth/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            LOGGER.info("Skipping JWT processing for /api/auth/login request.");
            CookieUtils.clearJwtCookie(response);
            LOGGER.info("JWT cookie cleared successfully.");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from Authorization header or cookies
        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String userEmail = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            LOGGER.info("Token extracted from the header.");
        } else {
            jwt = CookieUtils.extractTokenFromCookie(request);
            LOGGER.info("Token extracted from cookie.");
        }

        if (jwt == null) {
            LOGGER.warn("No JWT token found. Continuing without JWT validation.");
            filterChain.doFilter(request, response);
            return;
        }

        // Check if the token is revoked
        if (userService.isTokenRevoked(jwt)) {
            LOGGER.warn("Revoked token detected: {}", jwt);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"This token has been revoked.\"}");
            return;
        }

        // Extract user email from JWT
        userEmail = jwtService.extractUsername(jwt);
        LOGGER.info("Extracted email from JWT: {}", userEmail);

        // Check if the SecurityContext already contains a valid authentication
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        LOGGER.info("existingAuth : {} SecurityContext getAuthentication.", existingAuth);

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            LOGGER.info("SecurityContext is empty or invalid. Proceeding with JWT validation.");

            if (userEmail != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                LOGGER.info("User details loaded: {}, Authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());

                try {

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        LOGGER.debug("User authenticated successfully via JWT: {}", userEmail);
                        LOGGER.info("User authorities: {}", userDetails.getAuthorities());
                    } else {
                      RevokedToken revokedToken = userService.creatRevokedToken(jwt);
                      LOGGER.info("Invalid Token detected, It becomes a Revoked token {}", revokedToken);
                        throw new RuntimeException("Invalid JWT token.");
                    }
                } catch (Exception e) {
                    LOGGER.error("JWT validation failed: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"JWT validation failed.\"}");
                    return;
                }
            }
        } else {
            LOGGER.info("SecurityContext already contains a valid authentication. Skipping JWT validation.");
        }

        filterChain.doFilter(request, response);
    }
}

