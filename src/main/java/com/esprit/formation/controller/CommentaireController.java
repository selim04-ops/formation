package com.esprit.formation.controller;

import com.esprit.formation.dto.CommentaireResponse;
import com.esprit.formation.dto.CreateCommentaireRequest;
import com.esprit.formation.entities.Commentaire;
import com.esprit.formation.entities.Statut;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.ICommentaireService;
import com.esprit.formation.services.StatutService;
import com.esprit.formation.services.UserService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
@Tag(name = "Commentaire-End-Point", description = "Endpoints pour la gestion des commentaires")
public class CommentaireController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentaireController.class);


    private final ICommentaireService commentaireService;
    private final StatutService statutService;
    private final UserService userService;

    @Operation(summary = "Créer un commentaire", description = "Permet à tous les utilisateurs de créer un commentaire.")
    @PostMapping
    public ResponseEntity<?> createCommentaire(@RequestBody @Valid CreateCommentaireRequest createCommentaireRequest, Authentication authentication) {
        try {


            Optional<Statut> statutCheck = statutService.getStatutById(createCommentaireRequest.getStatutId());
            if(statutCheck.isEmpty()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST, "il n'ya pas un statut par l'id fournit");
            }

            User user = (User) authentication.getPrincipal();
            Commentaire commentaire =  Commentaire.builder()
                    .contenuCommentaire(createCommentaireRequest.getContent())
                    .statut(statutCheck.get())
                    .user(user)
                    .date(LocalDateTime.now())
                    .activeCommentaire(true)
                    .build();
            Commentaire createdCommentaire = commentaireService.createCommentaire(commentaire);
            CommentaireResponse commentaireResponse = commentaireService.mapToCommentaireResponse(createdCommentaire);
            return ResponseWrapper.success(commentaireResponse);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create commentaire");
        }
    }



    @Operation(summary = "Voir tous les commentaires paginés",
            description = "Permet aux administrateurs de voir tous les commentaires avec pagination.")
    @GetMapping("/commentaires/{statutId}")
    public ResponseEntity<?> getAllCommentairesByStatut(
            @PathVariable Long statutId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            // Verify statut exists
            if (statutService.getStatutById(statutId).isEmpty()) {
                return ResponseWrapper.error(HttpStatus.NOT_FOUND, "Statut non trouvé");
            }

            // Create pageable with sorting
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            // Get paginated comments
            Page<CommentaireResponse> response = commentaireService.getAllCommentairesByStatutId(statutId, pageable);

            // Build standardized response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("data", response.getContent());
            responseBody.put("currentPage", response.getNumber());
            responseBody.put("pageSize", response.getSize());
            responseBody.put("totalItems", response.getTotalElements());
            responseBody.put("totalPages", response.getTotalPages());

            return ResponseWrapper.success(responseBody);

        } catch (Exception e) {
            LOGGER.error("Erreur récupération commentaires: {}", e.getMessage());
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la récupération des commentaires");
        }
    }

    @Operation(summary = "Mettre à jour un commentaire", description = "Permet aux utilisateurs de mettre à jour un commentaire.")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCommentaire(
            @PathVariable Long id,
            @RequestBody @Valid CreateCommentaireRequest commentaireRequest,
            Authentication authentication) {

        try {
            Commentaire existingCommentaire = commentaireService.getCommentaireById(id)
                    .orElseThrow(() -> new RuntimeException("Commentaire non trouvé"));

            User user = (User) authentication.getPrincipal();
            if (!commentaireService.isOwnerOfCommentaire(id, user.getId()) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ADMIN") ||
                                    a.getAuthority().equals("SUPER_ADMIN"))) {
                return ResponseWrapper.error(HttpStatus.FORBIDDEN, "Permission refusée");
            }

            // Update only the necessary fields
            existingCommentaire.setContenuCommentaire(commentaireRequest.getContent());
            existingCommentaire.setDate(LocalDateTime.now());

            Commentaire savedCommentaire = commentaireService.createCommentaire(existingCommentaire);
            CommentaireResponse response = commentaireService.mapToCommentaireResponse(savedCommentaire);

            return ResponseWrapper.success(response);
        } catch (RuntimeException e) {
            return ResponseWrapper.error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Échec de la mise à jour");
        }
    }

    @Operation(summary = "Supprimer un commentaire", description = "Permet aux utilisateurs de supprimer un commentaire.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCommentaire(@PathVariable Long id, Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();

        try {
            // Check if the user is the owner or an admin/super_admin
            if (!commentaireService.isOwnerOfCommentaire(id, userId) && authentication.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("SUPER_ADMIN"))) {
                return ResponseWrapper.error(HttpStatus.FORBIDDEN, "You do not have permission to delete this commentaire");
            }

            commentaireService.deleteCommentaire(id);
            return ResponseWrapper.success("Commentaire deleted successfully");
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete commentaire");
        }
    }

    @Operation(summary = "Activer/Désactiver un commentaire",
            description = "Permet aux administrateurs d'activer/désactiver un commentaire.")
    @PostMapping("/toggle-commentaire/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> toggleActiveCommentaire(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        try {
            Boolean isActive = commentaireService.toggleActiveCommentaire(id, active);
            return ResponseWrapper.success(
                    Map.of(
                            "success", true,
                            "message", "Statut du commentaire mis à jour avec succès",
                            "activeCommentaire", isActive
                    )
            );
        } catch (RuntimeException e) {
            LOGGER.error("Erreur lors de la modification du statut du commentaire: {}", e.getMessage());
            return ResponseWrapper.error(
                    HttpStatus.NOT_FOUND,
                    e.getMessage() // Or use a generic message if preferred
            );
        } catch (Exception e) {
            LOGGER.error("Erreur inattendue: {}", e.getMessage());
            return ResponseWrapper.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Une erreur est survenue lors de la modification du statut"
            );
        }
    }
}
