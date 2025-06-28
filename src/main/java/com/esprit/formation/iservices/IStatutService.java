package com.esprit.formation.iservices;

import com.esprit.formation.dto.*;
import com.esprit.formation.entities.Statut;
import com.esprit.formation.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IStatutService {

    // CRUD Operations
    Statut createStatut(Statut statut);
    Optional<Statut> getStatutById(Long id);
    Page<StatutResponse> getAllStatuts(Pageable pageable);
    //Statut updateStatut(Long id, CreateStatutRequest updatedStatut);
    void deleteStatut(Long id);

    // Toggle Active Status
    Boolean toggleActiveStatus(Long id, Boolean active);
    boolean isOwnerOfStatut(Long statutId, Long userId);
    List<StatutResponse> mapToStatutResponseList(List<Statut> statutResponses);
    StatutResponse mapToStatutResponse(Statut statutResponse);




    }
