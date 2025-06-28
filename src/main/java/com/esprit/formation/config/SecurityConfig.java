package com.esprit.formation.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;


    public SecurityConfig(@Lazy OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;

    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/uploads/**",
                                "/config/**",
                                "/api/auth/**",
                                "/swagger-ui.html", // Autoriser Swagger UI
                                "/swagger-ui/**",   // Autoriser Swagger UI
                                "/v3/api-docs/**",
                                "/api/oauth2/**",
                                "/login/oauth2/code/**",
                                "/oauth2/authorization/**").permitAll() // Public endpoints
                        .requestMatchers( "/api/formations/**","/api/users/**", "/api/statuts/**", "/api/commentaires/**", "/api/coupons/**", "/ws-notifications/**","/api/notifications", "/api/cart/**", "/api/transactions","/api/stats", "/api/landing-page").hasAnyAuthority("ADMIN", "SUPER_ADMIN", "PARTICIPANT","FORMATEUR") //endpoint only for this
                        .anyRequest().authenticated() // All other endpoints require authentication
                )
                /*.csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // Disable CSRF
                        .ignoringRequestMatchers(

                                "/api/auth/**",
                                "/api/users/userState",
                                "/api/oauth2/**",
                                "/login/oauth2/code/**",
                                "/oauth2/authorization/**",
                                "/swagger-ui.html", // Ignorer CSRF pour Swagger UI
                                "/swagger-ui/**",   // Ignorer CSRF pour Swagger UI
                                "/v3/api-docs/**") // Ignore CSRF pour les endpoints publics
                )*/
                .csrf(csrf -> csrf.disable()) // Disable CSRF protection entirely
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless for most endpoints
                        .sessionFixation().migrateSession() // Enable session fixation protection
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2LoginSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Value("${cors.allowed.origin}")
    private String allowedOrigin;
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {


        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(allowedOrigin)); // Allow your React frontend
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allow these HTTP methods
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        configuration.setAllowCredentials(true); // Allow credentials (e.g., cookies)
        configuration.setExposedHeaders(Arrays.asList("Content-Disposition")); // Important for file downloads

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all /api endpoints
        source.registerCorsConfiguration("/uploads/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }




}