package com.esprit.formation.services;

import com.esprit.formation.dto.CommentaireResponse;
import com.esprit.formation.entities.Commentaire;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.ICommentaireService;
import com.esprit.formation.repository.CommentaireRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentaireService implements ICommentaireService {

    private final CommentaireRepository commentaireRepository;
    private final UserService userService;

    public CommentaireService(CommentaireRepository commentaireRepository, UserService userService) {
        this.commentaireRepository = commentaireRepository;
        this.userService = userService;
    }

    @Override
    public Commentaire createCommentaire(Commentaire commentaire) {
        return commentaireRepository.save(commentaire);
    }

    @Override
    public Optional<Commentaire> getCommentaireById(Long id) {
        return commentaireRepository.findById(id);
    }

    @Override
    public Page<CommentaireResponse> getAllCommentairesByStatutId(Long statutId, Pageable pageable) {
        return commentaireRepository.findActiveByStatutId(statutId, pageable)
                .map(this::mapToCommentaireResponse);
    }

    @Override
    public void deleteCommentaire(Long id) {
        commentaireRepository.deleteById(id);
    }

    @Override
    public Boolean toggleActiveCommentaire(Long id, Boolean active) {
        return commentaireRepository.findById(id)
                .map(commentaire -> {
                    commentaire.setActiveCommentaire(active);
                    Commentaire saved = commentaireRepository.save(commentaire);
                    return saved.getActiveCommentaire();
                })
                .orElseThrow(() -> new RuntimeException("Commentaire non trouvé"));
    }

    @Override
    public boolean isOwnerOfCommentaire(Long commentaireId, Long userId) {
        Commentaire commentaire = commentaireRepository.findById(commentaireId)
                .orElseThrow(() -> new RuntimeException("Statut not found with id: " + commentaireId));

        return commentaire.getUser().getId().equals(userId);
    }

    @Override
    public CommentaireResponse mapToCommentaireResponse(Commentaire commentaire) {

        User user = commentaire.getUser();
        return CommentaireResponse.builder()
                .id(commentaire.getId())
                .content(commentaire.getContenuCommentaire())
                .user(userService.mapToUserDtoPost(user))
                .date(commentaire.getDate())
                .statutId(commentaire.getStatut().getId())
                .build();
    }

    @Override
    public List<CommentaireResponse> mapToCommentaireResponsesList(List<Commentaire> commentaireResponses) {
        return commentaireResponses.stream()
                .map(this::mapToCommentaireResponse)
                .toList();
    }


}
