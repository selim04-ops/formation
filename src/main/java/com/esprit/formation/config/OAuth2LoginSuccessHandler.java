package com.esprit.formation.config;


import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IUserService;

import com.esprit.formation.services.CustomUserDetailsService;
import com.esprit.formation.services.JwtService;
import com.esprit.formation.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final IUserService userService;

    private final CustomUserDetailsService customUserDetailsService;

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Value("${cors.allowed.origin:http://localhost:8080}")
    private String frontendUrl;
    public OAuth2LoginSuccessHandler(JwtService jwtService, IUserService userService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        LOGGER.info("OAuth2LoginSuccessHandler triggered - authentication: {}", authentication);

        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            LOGGER.info("Checking and saving user with email: {}", email);

            if (email == null) {
                LOGGER.warn("L'utilisateur OAuth2 n'a pas d'adresse e-mail. Connexion refusée.");
                response.sendRedirect(frontendUrl + "?error=missing_email");
                return;
            }

            // Synchronize user creation/update
            userService.checkAndSaveUser(authentication);

            // Retrieve the user from the database after synchronization
            Optional<User> existingUser = userService.getUserByEmail(email);
            if (existingUser.isEmpty()) {
                LOGGER.error("Failed to retrieve or create user with email: {}", email);
                response.sendRedirect(frontendUrl + "?error=user_creation_failed");
                return;
            }

            User userExist = existingUser.get();

            // Ensure the user is active and has sufficient permissions
            if (!userExist.isEnabled()) {
                LOGGER.warn("L'utilisateur {} est inactif. Connexion refusée.", email);
                response.sendRedirect(frontendUrl + "?error=user_inactive");
                return;
            }

            if (!userExist.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("SUPER_ADMIN") || auth.getAuthority().equals("PARTICIPANT") || auth.getAuthority().equals("FORMATEUR"))) {
                LOGGER.warn("L'utilisateur {} n'a pas les autorisations nécessaires.", email);
                response.sendRedirect(frontendUrl + "?error=insufficient_permissions");
                return;
            }

            // Generate JWT token
            String token = jwtService.generateTokenForOauth2(userExist);
            LOGGER.info("JWT token generated for user: {}", email);

            // Set JWT cookie
            CookieUtils.addJwtCookie(response, token);

            LOGGER.info("this security context holder {}", SecurityContextHolder.getContext());

            // Clear OAuth2 SecurityContext
            SecurityContextHolder.clearContext();
            LOGGER.info("this security context holder {}", SecurityContextHolder.getContext());

            // Remove OAuth2AuthenticationToken from session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            }

            // Clear JSESSIONID cookie
            CookieUtils.clearJsessionIdCookie(response);
            LOGGER.info("JSESSIONID cookie cleared successfully.");

            // Set JWT-based authentication
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            LOGGER.info("Switched from OAuth2 to JWT-based authentication for user: {}", email);

            // Redirect to frontend
            response.sendRedirect(frontendUrl + "/oauth2-redirect");

        } catch (Exception e) {
            LOGGER.error("OAuth2 login failed: {}", e.getMessage(), e);
            response.sendRedirect(frontendUrl + "?error=oauth_failed");
            throw e;
        }
    }
}




