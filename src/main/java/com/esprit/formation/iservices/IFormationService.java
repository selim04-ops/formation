package com.esprit.formation.iservices;

import com.esprit.formation.dto.CreateFormationRequest;
import com.esprit.formation.dto.FormationResponse;
import com.esprit.formation.entities.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IFormationService {
    FormationResponse createFormation(CreateFormationRequest request, Role role);
    FormationResponse updateFormation(Long id, CreateFormationRequest request);
    void deleteFormation(Long id);
    FormationResponse getFormationById(Long id);
    Page<FormationResponse> getAllFormations(Pageable pageable);
    /*Page<FormationResponse> getFormationEnCours(LocalDate now, Pageable pageable);
    Page<FormationResponse> getFormationAvenir(LocalDate now, Pageable pageable);
    Page<FormationResponse> getFormationTerminer(LocalDate now, Pageable pageable);*/
    //Page<FormationResponse> getFormationsByUserId(Long userId, Pageable pageable);
    Boolean toggleFormationStatus(Long id, Boolean suprimeFormation);
    Page<FormationResponse> getUserFormations(Long userId, Pageable pageable);
    List<FormationResponse> getFormationsByIds(List<Long> formationIds);

}