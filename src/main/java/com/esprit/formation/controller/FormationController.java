package com.esprit.formation.controller;

import com.esprit.formation.dto.CreateFormationRequest;
import com.esprit.formation.dto.FormationResponse;
import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IFormationService;
import com.esprit.formation.services.UserService;
import com.esprit.formation.utils.ImageHandler;
import com.esprit.formation.utils.ResponseWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Formation-End-Point", description = "Endpoints pour la gestion des formations")
@RequiredArgsConstructor
@RequestMapping("/api/formations")
public class FormationController {

    private final IFormationService formationService;
    private static final Logger LOGGER = LoggerFactory.getLogger(FormationController.class);
    private final ObjectMapper objectMapper;
    private final JavaMailSenderImpl mailSender;
    private final UserService userService;

    @Operation(
            summary = "Créer une formation",
            description = "Créer une nouvelle formation avec des données JSON et des images (multipart/form-data)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Erreur de parsing JSON ou date invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la création de la formation")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createFormation(
            @Parameter(
                    description = "Données de la formation en JSON",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateFormationRequest.class))
            )
            @RequestPart("formationData") String formationDataStr,

            @Parameter(
                    description = "Fichiers image associés à la formation",
                    required = false,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            )
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        try {
            CreateFormationRequest request = objectMapper.readValue(formationDataStr, CreateFormationRequest.class);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            Role role = currentUser.getRole();
            List<String> imageUrls = ImageHandler.handleImages(images, request.getImageUrls());
            request.setImageUrls(imageUrls);

            FormationResponse response = formationService.createFormation(request, role);

            if (role == Role.FORMATEUR) {
                // Get all ADMIN and SUPER_ADMIN users
                List<User> adminUsers = userService.getByAllrole(List.of(Role.ADMIN, Role.SUPER_ADMIN));

                // Send email to each admin
                for (User admin : adminUsers) {
                    String subject = "New Formation Created by FORMATEUR";
                    String message = String.format(
                            "Dear %s,%n%nA new formation '%s' has been created by FORMATEUR %s (%s)%n%nPlease review it when possible.",
                            admin.getNomEtPrenom(),
                            response.getTitre(),
                            currentUser.getNomEtPrenom(),
                            currentUser.getEmail()
                    );
                    sendNotificationEmail(admin.getEmail(), subject, message);
                }
            }

            return ResponseWrapper.success(response);

        } catch (JsonProcessingException e) {
            LOGGER.error("Erreur de parsing JSON", e);
            return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Format JSON ou date invalide. Format attendu : dd-MM-yyyy");
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la création de la formation", e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la création de la formation : " + e.getMessage());
        }
    }

    private void sendNotificationEmail(String email, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom("nhoucem44@zohomail.com");
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
@Operation(summary = "Mettre à jour une formation", description = "Permet aux administrateurs de mettre à jour une formation.")
@PreAuthorize("hasAnyAuthority('ADMIN', 'FORMATEUR', 'SUPER_ADMIN')")
@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> updateFormation(
        @PathVariable Long id,
        @RequestPart("formationData") @Valid CreateFormationRequest request,
        @RequestPart(value = "images", required = false) List<MultipartFile> images) {

    try {
        // Handle images
        List<String> imageUrls = ImageHandler.handleImages(images, request.getImageUrls());


        // Update image URLs in request
        CreateFormationRequest updatedRequest = CreateFormationRequest.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .prix(request.getPrix())
                .niveau(request.getNiveau())
                .type(request.getType())
                .categorie(request.getCategorie())
                .imageUrls(imageUrls)
                .participantsIds(request.getParticipantsIds())
                .formateursIds(request.getFormateursIds())
                .build();

        FormationResponse response = formationService.updateFormation(id, updatedRequest);
        return ResponseWrapper.success(response);
    } catch (Exception e) {
        return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Échec de la mise à jour de la formation: " + e.getMessage());
    }
}

    @Operation(summary = "Supprimer une formation", description = "Permet aux administrateurs de supprimer une formation (marquée comme supprimée).")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'FORMATEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFormation(@PathVariable Long id) {
        try {
            formationService.deleteFormation(id);
            return ResponseWrapper.success("Formation marquée comme supprimée");
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Voir une formation", description = "Permet à tous les utilisateurs de voir une formation.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getFormationById(@PathVariable Long id) {
        try {
            FormationResponse response = formationService.getFormationById(id);
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "Récupérer des formations par leurs IDs",
            description = "Récupère une liste de formations à partir d'une liste d'identifiants")
    @PostMapping("/by-ids")
    public ResponseEntity<?> getFormationsByIds(@RequestBody List<Long> formationIds) {

        try {
            if (formationIds == null || formationIds.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "La liste des IDs ne peut pas être vide");
            }

            List<FormationResponse> formations = formationService.getFormationsByIds(formationIds);
            return ResponseWrapper.success(formations);
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la récupération des formations par IDs: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la récupération des formations: " + e.getMessage());
        }
    }

    @Operation(summary = "Voir toutes les formations", description = "Permet à tous les utilisateurs de voir toutes les formations (paginated).")
    @GetMapping
    public ResponseEntity<?> getAllFormations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateDebut") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            // Validate the 'sort' parameter
            if (!List.of("dateDebut", "dateFin", "prix").contains(sort)) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Invalid sort parameter: " + sort);
            }

            // Determine the sorting direction
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

            // Create the pageable object
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            // Fetch formations from the service
            Page<FormationResponse> response = formationService.getAllFormations(pageable);

            // Return the paginated response
            return ResponseWrapper.success(
                    Map.of(
                            "data", response.getContent(),
                            "currentPage", response.getNumber(),
                            "totalItems", response.getTotalElements(),
                            "totalPages", response.getTotalPages(),
                            "pageSize", response.getSize()
                    )
            );
        } catch (Exception e) {
            LOGGER.error("Error fetching formations: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la récupération des formations: " + e.getMessage());
        }

    }

    @Operation(summary = "Activer/Désactiver une formation",
            description = "Permet aux administrateurs d'activer/désactiver une formation")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/toggle-status/{id}")
    public ResponseEntity<?> desactiverFormation(
            @PathVariable Long id,
            @RequestParam Boolean suprimeFormation) {

        try {
            Boolean status = formationService.toggleFormationStatus(id, suprimeFormation);
            return ResponseWrapper.success(
                    Map.of(
                            "id", id,
                            "suprimeFormation", status
                    )
            );
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la mise à jour du statut de la formation: " + e.getMessage());
        }
    }


    @Operation(summary = "Récupérer les formations de l'utilisateur connecté",
            description = "Récupère les formations où l'utilisateur est participant ou formateur")
    @GetMapping("/my-formations")
    public ResponseEntity<?> getUserFormations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateDebut") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = ((User) authentication.getPrincipal()).getId();

            LOGGER.info("user id {}", userId);

            if (!List.of("dateDebut", "dateFin", "prix").contains(sort)) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Paramètre de tri invalide: " + sort);
            }

            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<FormationResponse> response = formationService.getUserFormations(userId, pageable);

            LOGGER.info("this is the request to send {}",response);


            return ResponseWrapper.success(
                    Map.of(
                            "data", response.getContent(),
                            "currentPage", response.getNumber(),
                            "totalItems", response.getTotalElements(),
                            "totalPages", response.getTotalPages(),
                            "pageSize", response.getSize()
                    )
            );
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la récupération des formations de l'utilisateur: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la récupération des formations: " + e.getMessage());
        }
    }
}