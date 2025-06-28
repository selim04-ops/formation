package com.esprit.formation.controller;

import com.esprit.formation.config.ActiveUser;
import com.esprit.formation.dto.AuthRequest;
import com.esprit.formation.dto.EnregisterRequest;
import com.esprit.formation.dto.ResetPaswordDto;
import com.esprit.formation.dto.UserRegistrationDTO;
import com.esprit.formation.entities.PasswordResetToken;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IAuthService;
import com.esprit.formation.iservices.IPasswordResetTokenService;
import com.esprit.formation.iservices.IUserService;
import com.esprit.formation.utils.CookieUtils;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentification-End-Point", description = "Endpoints pour la gestion de l'authentification")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final IAuthService authService;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final IPasswordResetTokenService passwordResetTokenService;
    private final JavaMailSender mailSender;
    private final ActiveUser activeUser;

    @Operation(summary = "Enregistrement", description = "Permet aux utilisateurs de s'enregistrer.")
    @PostMapping("/enregistrer")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO userDTO, BindingResult result) {
        try {
            // Step 1: Validate input
            if (result.hasErrors()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, result.getAllErrors().toString());
            }
            if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Les mots de passe ne correspondent pas.");
            }
            User user = new User();
            user.setNomEtPrenom(userDTO.getName());
            user.setEmail(userDTO.getEmail());
            user.setMotDePasse(userDTO.getPassword()); // Password will be encoded in the service
            User registeredUser = authService.enregistrer(user);
            if (registeredUser == null) {
                return ResponseWrapper.error(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà.");
            }
            Map<String, Object> responseData = Map.of(
                    "message", "Inscription réussie. Veuillez vérifier votre email pour activer votre compte.",
                    "email", registeredUser.getEmail()
            );
            return ResponseWrapper.success(responseData);
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'inscription : {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");
        }
    }

    @Operation(summary = "Verification par email",
            description = "Permet aux utilisateurs de confirmer son enregistrement via un code reçu par l'émail entré lors de l'enregistrement.")
    @PostMapping("/verify-email")
    public ResponseEntity<Boolean> verifyEmail(@RequestBody EnregisterRequest request) {
        return ResponseEntity.ok(authService.verifyEmailAndCode(request.getEmail(), request.getCode()));
    }

    @Operation(summary = "Se connectée", description = "Permet aux utilisateurs de se connecter via email et mot de passe.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {

        LOGGER.info("login process begin....");
        LOGGER.info("response contenu {}", response);

        try {
            // Step 1: Fetch user by email
            Optional<User> optionalUser = userService.getUserByEmail(request.getEmail());
            if (optionalUser.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
            }
            User user = optionalUser.get();
            // Step 2: Check if the user is active
            if (!user.isEnabled()) {
                return ResponseWrapper.error(HttpStatus.FORBIDDEN, "Votre compte est désactivé. Veuillez contacter l'administrateur.");
            }
            // Step 3: Validate password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseWrapper.error(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
            }
            // Step 4: Authenticate and generate JWT token
            String token = authService.authenticate(request.getEmail(), request.getPassword());
            if (token == null) {
                return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue lors de l'authentification.");
            }
            // Step 5: Add JWT token as an HTTP-only cookie
            CookieUtils.addJwtCookie(response, token);
            LOGGER.info("JWT cookie added in login endpoint.");

            // Step 6: Return success response
            Map<String, Object> responseData = Map.of(
                    "message", "Connexion réussie.",
                    "email", user.getEmail(),
                    "role", user.getRole()
            );
            return ResponseWrapper.success(responseData);
        } catch (RuntimeException e) {
            LOGGER.error("Erreur lors de la connexion : {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");
        }
    }

    @Operation(summary = "Réinitialisation du mot de passe",
            description = "Permet aux utilisateurs de réinitialiser leur mot de passe via un token.")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody EnregisterRequest emailRequest) {
        if (emailRequest.getEmail() == null || emailRequest.getEmail().isBlank()) {
            LOGGER.error("Missing email parameter!");
            return ResponseEntity.badRequest().body("Email is required");
        }
        String email = emailRequest.getEmail();
        LOGGER.info("the user {} want to reset password", email);
        // Validate email format
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Format d'email invalide.");
        }
        // Check if user exists and is active
        User user = userService.getUserByEmail(email).orElse(null);
        if (user == null) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Utilisateur non trouvé.");
        }
        if (!user.isEnabled()) {
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Utilisateur compte désactivé.");
        }
        LOGGER.info("debut de faire envoyé reset token....");
        String resetToken = passwordResetTokenService.checkForToken(user);
        PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(resetToken);
        if (passwordResetToken != null && passwordResetToken.isAvailable()) {
            String emailBody = "copier ce code puis l'utiliser pour réinitialiser votre mot de passe: " + resetToken;
            mailSender.send(constructEmail("Réinitialisation du mot de passe", emailBody, user));
            LOGGER.info("Lien de réinitialisation envoyé à l'utilisateur: {}", user.getEmail());
            return ResponseWrapper.success("Email de réinitialisation envoyé avec succès.");
        }
        return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la création du jeton.");
    }

    @Operation(summary = "Réinitialisation du mot de passe",
            description = "Permet aux utilisateurs de réinitialiser leur mot de passe via un token envoyé par mail.")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPaswordDto request) {
        try {
            String newPassword = request.getNewpassword();
            // Validate token
            if (request.getToken() == null || request.getToken().isBlank()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Le token est requis.");
            }

            // Validate new password
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Le nouveau mot de passe est requis.");
            }

            if (newPassword.length() < 6) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 6 caractères.");
            }

            PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(request.getToken());

            // Check token validity
            if (passwordResetToken == null) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Token invalide.");
            }

            if (passwordResetToken.getExpiryDate().before(new Date())) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Token expiré.");
            }

            if (!passwordResetToken.isAvailable()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Token déjà utilisé.");
            }

            User user = passwordResetToken.getUser();


            // Update password
            userService.updateUserPassword(user, newPassword);

            // Invalidate the token
            passwordResetTokenService.updatePasswordResetToken(passwordResetToken);

            LOGGER.info("Password reset successfully for user: {}", user.getEmail());
            return ResponseWrapper.success("Mot de passe réinitialisé avec succès.");

        } catch (Exception e) {
            LOGGER.error("Error resetting password: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue lors de la réinitialisation du mot de passe.");
        }
    }

    @Operation(summary = "Déconnexion", description = "Permet aux utilisateurs de se déconnecter.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletResponse response,
            @CookieValue(value = "jwt", required = false) String token,
            HttpServletRequest request
    ) {
        try {
            userService.performLogout(token, request, response);

            Map<String, Object> responseData = Map.of(
                    "message", "Déconnexion réussie.",
                    "timestamp", LocalDateTime.now()
            );
            return ResponseWrapper.success(responseData);
        } catch (RuntimeException e) {
            LOGGER.error("Erreur lors de la déconnexion : {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");
        }
    }

    @Operation(summary = "Get Current User", description = "Renvoie les détails de l'utilisateur actuellement authentifié.")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserDetails() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                LOGGER.info("pas de token trouvé dans la request....");
            return ResponseWrapper.error(HttpStatus.UNAUTHORIZED, "Token d'authentification manquant ou invalide.");
            }
            LOGGER.info("debut de l'extraction de l'email of user....");
            String email;
            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                email = oauth2User.getAttribute("email"); // Extract the email attribute
                if (email == null) {
                    return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Adresse e-mail manquante dans le jeton OAuth2.");
                }
            } else {
                email = authentication.getName();
            }
            User user = userService.getUserByEmail(email).orElse(null);
            if (user == null) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "Utilisateur non trouvé.");
            }
            if (!userService.validateRole(String.valueOf(user.getRole()))) {
                return ResponseWrapper.error(HttpStatus.FORBIDDEN, "Accès non autorisé.");
            }
            boolean vide = user.getPassword() == null;

            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("email", user.getEmail());
            userDetails.put("role", user.getRole());
            userDetails.put("isActive", user.isEnabled());
            userDetails.put("nomEtPrenom", user.getNomEtPrenom());
            userDetails.put("imgUrl", user.getImgUrl());
            userDetails.put("cin", user.getCin());
            userDetails.put("phoneNumber", user.getPhoneNumber());
            userDetails.put("vide", vide);
            return ResponseWrapper.success(userDetails);
        } catch (RuntimeException e) {
            LOGGER.error("Erreur lors de la vérification du jeton ou de l'extraction de l'utilisateur: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Erreur inattendue: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue.");
        }
    }

    private SimpleMailMessage constructEmail(String subject, String body, User user) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setFrom("nhoucem44@zohomail.com");
        email.setText(body);
        email.setTo(user.getEmail());
        return email;
    }
}