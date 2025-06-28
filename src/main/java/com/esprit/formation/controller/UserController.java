package com.esprit.formation.controller;

import com.esprit.formation.config.ActiveUser;
import com.esprit.formation.dto.AuthRequest;
import com.esprit.formation.dto.UserDTO;
import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IUserService;
import com.esprit.formation.services.FileStorageService;
import com.esprit.formation.utils.ImageHandler;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User-End-Point", description = "Endpoints pour la gestion de l'utilisateur")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final IUserService userService;
    private final ActiveUser activeUser;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Voir utilisateur par son email",
            description = "Permet aux utilisateurs de voir un utilisateur par son email.")
    @GetMapping("/get/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> userOptional = userService.getUserByEmail(email);
            if (userOptional.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "User not found");
            }

            User user = userOptional.get();
            UserDTO userDTO = userService.mapToUserDTO(user);
            return ResponseWrapper.success(userDTO);
        } catch (Exception e) {
            LOGGER.error("Error fetching user by email: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }


    @Operation(summary = "Get users by their IDs")
    @PostMapping("/get-by-ids")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getUsersByIds(@RequestBody UserIdsRequest request) {
        try {
            if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseWrapper.success(Collections.emptyList());
            }

            Set<User> users = userService.getUserByIds(request.getIds());

            List<UserDTO> userDTOs = users.stream()
                    .map(userService::mapToUserDTO)
                    .collect(Collectors.toList());

            return ResponseWrapper.success(userDTOs);
        } catch (Exception e) {
            LOGGER.error("Error fetching users by IDs: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve users: " + e.getMessage());
        }
    }

    @Operation(summary = "Mise à jour utilisateur",
            description = "Permet aux utilisateurs de mettre à jour un utilisateur.")
    @PutMapping(value = "/{email}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUser(
            @PathVariable String email,
            @RequestPart("userDTO") UserDTO updatedUserDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        LOGGER.info("Received request to update user with email: {}", email);
        LOGGER.info("UserDTO: {}", updatedUserDTO);

        try {
            // Fetch the user by email
            Optional<User> userOptional = userService.getUserByEmail(email);
            if (userOptional.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "User not found");
            }

            User user = userOptional.get();

            // Update user fields from DTO
            if (updatedUserDTO.getNomEtPrenom() != null) {
                user.setNomEtPrenom(updatedUserDTO.getNomEtPrenom());
            }

            if (updatedUserDTO.getCin() != null) {
                user.setCin(updatedUserDTO.getCin());
            }
            if (updatedUserDTO.getRole() != null) {
                user.setRole(updatedUserDTO.getRole());
            }

            if (updatedUserDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(updatedUserDTO.getPhoneNumber());
            }

            // Handle password update (only if provided)
            if (updatedUserDTO.getPassword() != null && !updatedUserDTO.getPassword().isEmpty()) {
                user.setMotDePasse(passwordEncoder.encode(updatedUserDTO.getPassword()));
            }

            // Convert single imgUrl to List for ImageHandler
            List<String> existingImageUrls = updatedUserDTO.getImgUrl() != null && !updatedUserDTO.getImgUrl().isEmpty()
                    ? Collections.singletonList(updatedUserDTO.getImgUrl())
                    : Collections.emptyList();

            // Handle images
            List<String> imageUrls = ImageHandler.handleImages(images, existingImageUrls);

            // Set the first image URL (or null if empty)
            user.setImgUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));

            // Save the updated user
            User savedUser = userService.saveUser(user);

            // Return success response
            return ResponseWrapper.success(userService.mapToUserDTO(savedUser));

        } catch (Exception e) {
            LOGGER.error("Error updating user: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }

    @Operation(summary = "Voir les utilisateurs actifs",
            description = "Permet aux utilisateurs de voir les utilisateurs connectés.")
    @GetMapping("/active-users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> getActiveUsers() {
        try {
            List<String> activeUsers = new ArrayList<>(activeUser.activeUsers.values());
            LOGGER.info("Fetched {} active users.", activeUsers.size());
            return ResponseWrapper.success(activeUsers);
        } catch (Exception e) {
            LOGGER.error("Error fetching active users: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }

    @Operation(summary = "Voir tous les utilisateurs",
            description = "Permet aux administrateurs de voir la liste de tous les utilisateurs.")
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllEnabledUsers() {
        try {
            List<User> users = userService.getAllEnabledUsers();
            if (users.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NO_CONTENT, "No users found");
            }

            // Map the list of User entities to UserDTO objects
            List<UserDTO> userDTOs = users.stream()
                    .map(userService::mapToUserDTO)
                    .toList();

            return ResponseWrapper.success(userDTOs);
        } catch (Exception e) {
            LOGGER.error("Error fetching all users: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }

    @PostMapping("/allByRole")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllByRole(@RequestBody String roleFilter) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LOGGER.info("this is the role a fetcher{}", roleFilter);
        if (authentication == null || !authentication.isAuthenticated()) {
            LOGGER.info("absance de token....");
            return ResponseWrapper.error(HttpStatus.UNAUTHORIZED, "Token d'authentification manquant ou invalide.");
        }
        LOGGER.info("Received raw role filter from frontend: {}", roleFilter);

        try {
            // Sanitize the input by removing extra quotes and trimming whitespace
            String sanitizedRole = roleFilter.replaceAll("^\"|\"$", "").trim();
            LOGGER.info("Sanitized role filter: {}", sanitizedRole);

            // Convert the sanitized string to a Role enum
            Role role;
            try {
                role = Role.valueOf(sanitizedRole.toUpperCase());
                LOGGER.info("Parsed role: {}", role);
            } catch (IllegalArgumentException e) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Invalid role: " + sanitizedRole);
            }

            // Fetch users by role
            List<User> users = userService.getAllByRole(role);
            if (users.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NO_CONTENT, "No users found for role: " + role);
            }

            // Map the list of User entities to UserDTO objects
            List<UserDTO> userDTOs = users.stream()
                    .map(userService::mapToUserDTO)
                    .toList();

            return ResponseWrapper.success(userDTOs);
        } catch (Exception e) {
            LOGGER.error("Error fetching all users: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }

    @Operation(summary = "Ajouter un utilisateur",
            description = "Permet aux administrateurs d'ajouter un nouvel utilisateur.")
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> addUser(
            @RequestPart("userDTO") @Valid UserDTO userDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        LOGGER.info("Received userDTO: {}", userDTO);
        try {
            // Validate the incoming DTO
            if (userDTO.getEmail() == null || userDTO.getNomEtPrenom() == null) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Email and name are required");
            }
            // Check if user exists
            if (userService.getUserByEmail(userDTO.getEmail()).isPresent()) {
                return ResponseWrapper.error(HttpStatus.CONFLICT, "Email already exists");
            }
            // Handle image
            if (imageFile != null && !imageFile.isEmpty()) {
                String savedImageUrl = ImageHandler.saveImage(imageFile);
                userDTO.setImgUrl(savedImageUrl);
            } else if (userDTO.getImgUrl() == null || userDTO.getImgUrl().isEmpty()) {
                userDTO.setImgUrl("https://ui-avatars.com/api/?name=" +
                        URLEncoder.encode(userDTO.getNomEtPrenom(), StandardCharsets.UTF_8) +
                        "&background=random");
            }

            User newUser = userService.mapToUserEntity(userDTO);
            User savedUser = userService.saveUser(newUser);

            return ResponseWrapper.success(userService.mapToUserDTO(savedUser));
        } catch (Exception e) {
            LOGGER.error("Error adding user", e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating user");
        }
    }


    @Operation(summary = "Mettre à jour l'état actif/inactif d'un utilisateur",
            description = "Permet aux administrateurs de mettre à jour l'état actif/inactif d'un utilisateur.")
    @PutMapping("/userState")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateUserState(@RequestBody AuthRequest statusRequest) {

        LOGGER.info("Received payload: {}", statusRequest);
        LOGGER.info("Email: {}, IsActive: {}", statusRequest.getEmail(), statusRequest.getIsActive());
        String email=statusRequest.getEmail();
        LOGGER.info("this is the actuel user email {} ", email);

        boolean isActive = statusRequest.getIsActive();
        LOGGER.info("this is the actuel status of user {} is {}", statusRequest, isActive);
        try {
            // Validate input
            if (email == null || email.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Email is required");
            }

            // Check if the user exists
            Optional<User> existingUser = userService.getUserByEmail(email);
            if (existingUser.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "User not found");
            }
            LOGGER.info("this is the actuel status of user {}", isActive);
            // Update the user's active state
            User updatedUser = userService.updateUserActiveState(email, isActive);
            LOGGER.info("now the status of user {}", updatedUser.getIsActive());


            // Return the updated user as DTO
            return ResponseWrapper.success(userService.mapToUserDTO(updatedUser));
        } catch (Exception e) {
            LOGGER.error("Error updating user state: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }


    @Operation(summary = "Supprimer un utilisateur",
            description = "Permet aux administrateurs de supprimer un utilisateur par son email.")
    @DeleteMapping("/{email}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        LOGGER.info("Received request to delete user with email: {}", email);

        try {
            // Check if the user exists
            Optional<User> userOptional = userService.getUserByEmail(email);
            if (userOptional.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "User not found");
            }

            // Delete the user
            userService.deleteUserByEmail(email);

            // Return success response
            return ResponseWrapper.success("User deleted successfully");

        } catch (Exception e) {
            LOGGER.error("Error deleting user: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserIdsRequest {
        private List<Long> ids;
    }
}
