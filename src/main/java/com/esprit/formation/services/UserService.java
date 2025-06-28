package com.esprit.formation.services;

import com.esprit.formation.dto.UserDTO;
import com.esprit.formation.entities.RevokedToken;
import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IUserService;
import com.esprit.formation.repository.RevokedTokenRepository;
import com.esprit.formation.repository.UserRepository;
import com.esprit.formation.utils.CookieUtils;
import com.esprit.formation.utils.WebSocketSessionRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService implements IUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final WebSocketSessionRegistry sessionRegistry;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    public UserService(WebSocketSessionRegistry sessionRegistry, UserRepository userRepository, PasswordEncoder passwordEncoder, RevokedTokenRepository revokedTokenRepository, JwtService jwtService) {
        this.sessionRegistry = sessionRegistry;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
    }

    //user crud-----------------------------------------------------------------------------

    @Override
    public User saveUser(User user) {
        LOGGER.info("Registering new user with email: {}", user.getEmail());
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUserActiveState(String email, boolean isActive) {
        // Fetch the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Update the isActive field
        user.setIsActive(isActive);

        // Save the updated user
        return userRepository.save(user);
    }

    @Override
    public void deleteUserByEmail(String email) {
        // Fetch the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Perform the deletion
        userRepository.delete(user);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    @Override
    public Set<User> getUserByIds(List<Long> ids) {
        return userRepository.findAllByIdInAndIsActiveTrue(ids);
    }



    @Override
    public Boolean validateRole(String role) {
        return Role.isValidRole(role);
    }

    @Override
    public List<User> getAllEnabledUsers() {
        return userRepository.findAllByIsActiveTrue();
    }

    @Override
    public List<User> getAllByRole(Role role) {
        return userRepository.findAllByRole(role);
    }

    @Override
    public List<User> getAllDeleteddUsers() {
        return userRepository.findAllByIsActiveFalse();
    }
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
//------------------------------------------------------------------------------

// handle the user logout-----------------------------------------------------
    @Override
    public void performLogout(String token, HttpServletRequest request, HttpServletResponse response) {
        // Step 1: Revoke the JWT token
        if (token != null) {
            this.creatRevokedToken(token);
            LOGGER.info("A logout action is done, this token {} become revokedToken", token);
        }

        // Step 2: Clear JWT cookie
        CookieUtils.clearJwtCookie(response);

        // Step 3: Clear JSESSIONID cookie (if it exists)
        CookieUtils.clearJsessionIdCookie(response);

        // Step 4: Invalidate the HTTP session (optional but recommended)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            LOGGER.info("HTTP session invalidated.");
        }
    }


    public List<User> getByAllrole(List<Role> roles) {
        return userRepository.findByRoleIn(roles);
    }

    // to set a new password for user-----------------------------------
    @Override
    public User updateUserPassword(User user, String newPassword) {
        User user1 = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.FALSE.equals(user1.getIsActive())) {
            throw new RuntimeException("User is not active");
        }

        user1.setMotDePasse(passwordEncoder.encode(newPassword));
        userRepository.save(user1);
        return user1;
    }

// creation of user connected via facebook or google-------------------
    @Override
    public void checkAndSaveUser(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            LOGGER.error("Authentication principal is not an instance of OAuth2User");
            return;
        }

        String name = oauthUser.getAttribute("name");
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String email = oauthUser.getAttribute("email");

        LOGGER.info("Checking OAuth2 user: {} (provider: {})", name, provider);

        if (email == null) {
            LOGGER.warn("L'utilisateur OAuth2 n'a pas d'adresse e-mail. Impossible de l'enregistrer.");
            return;
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.isEnabled()) {
                LOGGER.warn("L'utilisateur {} est inactif. Connexion refusée.", email);
                return;
            }
            // Update user details if necessary
            updateUserDetails(user, oauthUser);
        } else {
            // Create a new user
            LOGGER.info("Création d'un nouvel utilisateur avec l'email: {}", email);
            User newUser = createUserFromOAuth(oauthUser);
            userRepository.save(newUser);
        }
    }

    private User createUserFromOAuth(OAuth2User oauthUser) {
        User newUser = new User();
        newUser.setEmail(oauthUser.getAttribute("email"));
        newUser.setNomEtPrenom(oauthUser.getAttribute("name"));
        newUser.setIsActive(true); // Default active status

        if (oauthUser.getAttribute("sub") != null) {
            newUser.setGoogleId(oauthUser.getAttribute("sub"));
            newUser.setImgUrl(oauthUser.getAttribute("picture"));
        } else if (oauthUser.getAttribute("id") != null) {
            newUser.setFacebookId(oauthUser.getAttribute("id"));
            String fbImg = extractFacebookImageUrl(oauthUser);
            newUser.setImgUrl(fbImg);
        }

        return newUser;
    }

    private void updateUserDetails(User user, OAuth2User oauthUser) {
        boolean updated = false;

        if (oauthUser.getAttribute("sub") != null) { // Google
            String googleId = oauthUser.getAttribute("sub");
            if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                LOGGER.info("Mise à jour de l'ID Google pour l'utilisateur {}", user.getEmail());
                user.setGoogleId(googleId);
                updated = true;
            }

            //String picture = oauthUser.getAttribute("picture");
            String picture = user.getImgUrl();

            if (picture == null) {
                user.setImgUrl(oauthUser.getAttribute("picture"));
                updated = true;
            }
        } else if (oauthUser.getAttribute("id") != null) { // Facebook
            String facebookId = oauthUser.getAttribute("id");
            if (user.getFacebookId() == null || !user.getFacebookId().equals(facebookId)) {
                LOGGER.info("Mise à jour de l'ID Facebook pour l'utilisateur {}", user.getEmail());
                user.setFacebookId(facebookId);
                updated = true;
            }

            String fbImageUrl = extractFacebookImageUrl(oauthUser);
            if (fbImageUrl != null && !fbImageUrl.equals(user.getImgUrl())) {
                user.setImgUrl(fbImageUrl);
                updated = true;
            }
        }

        // Add name if it's null
        if (user.getNomEtPrenom() == null && oauthUser.getAttribute("name") != null) {
            user.setNomEtPrenom(oauthUser.getAttribute("name"));
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }
    }

    private String extractFacebookImageUrl(OAuth2User oauthUser) {
        Object pictureAttr = oauthUser.getAttribute("picture");
        if (pictureAttr instanceof Map<?, ?> pictureMap && pictureMap.containsKey("data")) {
            Object dataAttr = pictureMap.get("data");
            if (dataAttr instanceof Map<?, ?> dataMap) {
                return (String) dataMap.get("url");
            }
        }
        return null;
    }
    //------------------------------------------------------------------------------


    public List<String> findConnectedUsersByRoles(List<String> roles) {
        return sessionRegistry.findUsernamesByRoles(roles);
    }

// other methode for user management------------------------------------------------
    @Override
    public User hasGoogleId(User user, String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    @Override
    public User hasFacebookId(User user, String facebookId) {
        return userRepository.findByFacebookId(facebookId);
    }

    @Override
    public RevokedToken creatRevokedToken (String token){
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(token);
        return revokedTokenRepository.save(revokedToken);
    }

    @Override
    public boolean isTokenRevoked(String token) {
        // Check if the token is expired
        if (jwtService.isTokenExpired(token)) {
            creatRevokedToken(token); // Revoke expired tokens
            return true;
        }

        // Check if the token is explicitly revoked
        return revokedTokenRepository.existsById(token);
    }



// other methode to map user -------------------------------------------------------------
@Override
    public UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nomEtPrenom(user.getNomEtPrenom())
                .role(user.getRole())
                .imgUrl(user.getImgUrl())
                .isActive(user.getIsActive())
                .cin(user.getCin())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
@Override
    public User mapToUserEntity(UserDTO userDTO) {
        return User.builder()
                .email(userDTO.getEmail())
                .nomEtPrenom(userDTO.getNomEtPrenom())
                .imgUrl(userDTO.getImgUrl())
                .role(userDTO.getRole())
                .isActive(true) // Assuming new users are enabled by default
                .motDePasse(passwordEncoder.encode(userDTO.getPassword()))
                .build();
    }

    @Override
    public UserDTO mapToUserDtoPost(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nomEtPrenom(user.getNomEtPrenom())
                .imgUrl(user.getImgUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }

}
