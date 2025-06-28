package com.esprit.formation.services;

import com.esprit.formation.dto.*;
import com.esprit.formation.entities.Statut;
import com.esprit.formation.iservices.IStatutService;
import com.esprit.formation.repository.StatutRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StatutService implements IStatutService {

    private final StatutRepository statutRepository;
    private final UserService userService;
    private final CommentaireService commentaireService;

    public StatutService(StatutRepository statutRepository, UserService userService, CommentaireService commentaireService) {
        this.statutRepository = statutRepository;
        this.userService = userService;
        this.commentaireService = commentaireService;
    }
    @Transactional
    @Override
    public Statut createStatut(Statut statut) {
        return statutRepository.save(statut);
    }

    @Override
    public Optional<Statut> getStatutById(Long id) {
        return statutRepository.findById(id);
    }
    @Transactional(readOnly = true)
    @Override
    public Page<StatutResponse> getAllStatuts(Pageable pageable) {
        return statutRepository.findByActiveStatusTrue(pageable)
                .map(this::mapToStatutResponse);
    }


    @Override
    public void deleteStatut(Long id) {
        statutRepository.deleteById(id);
    }

    @Override
    public Boolean toggleActiveStatus(Long id, Boolean active) {
        Statut statut = statutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Statut not found with id: " + id));

        statut.setActiveStatus(active);
        Statut savedStatut = statutRepository.save(statut);
        return savedStatut.getActiveStatus();
    }

@Override
    public boolean isOwnerOfStatut(Long statutId, Long userId) {
        Statut statut = statutRepository.findById(statutId)
                .orElseThrow(() -> new RuntimeException("Statut not found with id: " + statutId));
        return statut.getUser().getId().equals(userId);
    }



    @Override
    public StatutResponse mapToStatutResponse(Statut statut) {
        return StatutResponse.builder()
                .id(statut.getId())
                .content(statut.getContenuStatut())
                .user(userService.mapToUserDtoPost(statut.getUser()))
                .images(statut.getImages())
                .date(statut.getDate())
                .activeStatus(statut.getActiveStatus())
                .commentaires(statut.getCommentaires() != null ?
                        statut.getCommentaires().stream()
                                .map(commentaireService::mapToCommentaireResponse)
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
    @Override
    public List<StatutResponse> mapToStatutResponseList(List<Statut> statutResponses) {
        return statutResponses.stream()
                .map(this::mapToStatutResponse)
                .toList();
    }


}
