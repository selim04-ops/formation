package com.esprit.formation.iservices;

import com.esprit.formation.dto.CommentaireResponse;
import com.esprit.formation.entities.Commentaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ICommentaireService {
    // CRUD Operations
    Commentaire createCommentaire(Commentaire commentaire);
    Optional<Commentaire> getCommentaireById(Long id);
    Page<CommentaireResponse> getAllCommentairesByStatutId(Long statutId, Pageable pageable);
    //Commentaire updateCommentaire(Long id, Commentaire updatedCommentaire);
    void deleteCommentaire(Long id);

    // Toggle Active Commentaire
    Boolean toggleActiveCommentaire(Long id, Boolean active);
     boolean isOwnerOfCommentaire(Long commentaireId, Long userId);

    CommentaireResponse mapToCommentaireResponse(Commentaire commentaire);
    List<CommentaireResponse> mapToCommentaireResponsesList(List<Commentaire> commentaireResponses);

    }
