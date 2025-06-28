package com.esprit.formation.controller;

import com.esprit.formation.dto.CreateStatutRequest;
import com.esprit.formation.dto.StatutResponse;
import com.esprit.formation.entities.Statut;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IStatutService;
import com.esprit.formation.utils.ImageHandler;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/statuts")
@RequiredArgsConstructor
@Tag(name = "Statut-End-Point", description = "Endpoints pour la gestion des statuts")
public class StatutController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatutController.class);


    private final IStatutService statutService;

    @Operation(summary = "Créer un statut", description = "Permet à tous les utilisateurs de créer un statut.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createStatut(
            @RequestPart("statutData") @Valid CreateStatutRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
LOGGER.info("Create new statut {}", request);
        try {
            // 1. Validate input
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "Le contenu ne peut pas être vide");
            }

            // 2. Get authenticated user
            User user = (User) authentication.getPrincipal();

            // 3. Handle images
            List<String> imageUrls =  ImageHandler.handleImages(images, request.getImageUrls());
            LOGGER.info("this is the image url {}", imageUrls);
            // 4. Build and save statut
            Statut statut = Statut.builder()
                    .contenuStatut(request.getContent())
                    .user(user)
                    .images(imageUrls)
                    .date(LocalDateTime.now())
                    .activeStatus(true)
                    .commentaires(new ArrayList<>())
                    .build();

            LOGGER.info("this is the statut{}", statut);

            Statut createdStatut = statutService.createStatut(statut);
            StatutResponse response = statutService.mapToStatutResponse(createdStatut);

            return ResponseWrapper.success(response);

        } catch (Exception e) {
            LOGGER.error("Erreur lors de la création du statut: {}", e.getMessage(), e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la création du statut: " + e.getMessage());
        }
    }

    @Operation(summary = "Mettre à jour un statut", description = "Permet aux utilisateurs de mettre à jour un statut.")
    @PutMapping(value = "/{idStatut}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateStatut(
            @PathVariable Long idStatut,
            @RequestPart("statutData") @Valid CreateStatutRequest updatedStatut,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {

        try {
            // Check if statut exists
            Statut existingStatut = statutService.getStatutById(idStatut)
                    .orElseThrow(() -> new RuntimeException("Statut non trouvé"));

            // Check permissions
            User user = (User) authentication.getPrincipal();
            if (!statutService.isOwnerOfStatut(idStatut, user.getId()) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                    a.getAuthority().equals("SUPER_ADMIN"))) {
                return ResponseWrapper.error(HttpStatus.FORBIDDEN, "Permission refusée");
            }

            // Handle images
            List<String> imageUrls = ImageHandler.handleImages(images, updatedStatut.getImageUrls());

            // Update fields
            existingStatut.setContenuStatut(updatedStatut.getContent());
            existingStatut.setImages(imageUrls);
            existingStatut.setDate(LocalDateTime.now());

            // Save and return
            Statut updated = statutService.createStatut(existingStatut);
            StatutResponse response = statutService.mapToStatutResponse(updated);

            return ResponseWrapper.success(response);

        } catch (RuntimeException e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Échec de la mise à jour");
        }
    }


    @Operation(summary = "Voir un statut par ID", description = "Permet aux utilisateurs de voir un statut par son ID.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getStatutById(@PathVariable Long id) {
        try {
            Optional<Statut> statut =statutService.getStatutById(id);
            if (statut.isEmpty()){
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "le statut n'exist pas");
            }
            StatutResponse statutResponse = statutService.mapToStatutResponse(statut.get());
            return ResponseWrapper.success(statutResponse);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "Voir tous les statuts (paginated)",
            description = "Permet aux administrateurs de voir tous les statuts avec pagination.")
    @GetMapping
    public ResponseEntity<?> getAllStatuts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<StatutResponse> response = statutService.getAllStatuts(pageable);

            if (response.isEmpty()) {
                return ResponseWrapper.success(
                        Map.of(
                                "data", Collections.emptyList(),
                                "currentPage", response.getNumber(),
                                "totalItems", response.getTotalElements(),
                                "totalPages", response.getTotalPages(),
                                "pageSize", response.getSize()
                        )
                );
            }

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
            LOGGER.error("Error fetching statuts: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la récupération des statuts");
        }

    }


    @Operation(summary = "Supprimer un statut", description = "Permet aux utilisateurs de supprimer un statut.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStatut(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LOGGER.info("this is the principal {}", authentication.getPrincipal());
        Long userId = ((User) authentication.getPrincipal()).getId();

        try {
            // Check if the user is the owner or an admin/super_admin
            if (!statutService.isOwnerOfStatut(id, userId) && !authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("SUPER_ADMIN"))) {
                return ResponseWrapper.error(HttpStatus.FORBIDDEN, "You do not have permission to delete this statut");
            }

            statutService.deleteStatut(id);
            return ResponseWrapper.success("Statut deleted successfully");
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete statut");
        }
    }

    @Operation(summary = "Activer/Désactiver un statut", description = "Permet aux administrateurs d'activer/désactiver un statut.")
    @PostMapping("/toggle-status/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> toggleActiveStatus(@PathVariable Long id, @RequestParam Boolean active) {
        try {
            Boolean isActive = statutService.toggleActiveStatus(id, active);
            return ResponseWrapper.success(Map.of("activeStatus", isActive));
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to toggle active status");
        }
    }




}