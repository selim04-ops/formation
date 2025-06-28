package com.esprit.formation.services;

import com.esprit.formation.dto.CouponResponse;
import com.esprit.formation.dto.CreateFormationRequest;
import com.esprit.formation.dto.FormationResponse;
import com.esprit.formation.entities.Coupon;
import com.esprit.formation.entities.Formation;
import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IFormationService;
import com.esprit.formation.repository.FormationRepository;
import com.esprit.formation.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class FormationService implements IFormationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormationService.class);

    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public FormationService(FormationRepository formationRepository,
                            UserRepository userRepository,
                            UserService userService) {
        this.formationRepository = formationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public FormationResponse createFormation(CreateFormationRequest request, Role role) {
        Formation formation = mapToEntity(request);

        // Handle participants
        if (request.getParticipantsIds() != null && !request.getParticipantsIds().isEmpty()) {
            Set<User> participants = userRepository.findAllByIdInAndRole(
                    request.getParticipantsIds(), Role.PARTICIPANT);
            participants.forEach(formation::addParticipant);
        }

        // Handle formateurs
        if (request.getFormateursIds() != null && !request.getFormateursIds().isEmpty()) {
            Set<User> formateurs = userRepository.findAllByIdInAndRole(
                    request.getFormateursIds(), Role.FORMATEUR);
            formateurs.forEach(formation::addFormateur);
        }
        if (role == Role.FORMATEUR){
            formation.setActive(false);

        }

        formation.updateEtatFormation();
        Formation savedFormation = formationRepository.save(formation);
        return mapToFormationResponse(savedFormation);
    }

    @Override
    public FormationResponse updateFormation(Long id, CreateFormationRequest request) {
        Formation formation = verifyAndGetFormation(id);

        // Update basic properties
        formation.setTitre(request.getTitre());
        formation.setDescription(request.getDescription());
        formation.setDateDebut(request.getDateDebut());
        formation.setDateFin(request.getDateFin());
        formation.setPrix(BigDecimal.valueOf(request.getPrix()));
        formation.setNiveau(request.getNiveau());
        formation.setType(request.getType());
        formation.setCategorie(request.getCategorie());

        // Handle images
        if (request.getImageUrls() != null) {
            formation.setImages(request.getImageUrls());
        }

        // Update participants
        if (request.getParticipantsIds() != null) {
            updateParticipants(formation, request.getParticipantsIds());
        }

        // Update formateurs
        if (request.getFormateursIds() != null) {
            updateFormateurs(formation, request.getFormateursIds());
        }

        formation.updateEtatFormation();
        Formation updatedFormation = formationRepository.save(formation);
        return mapToFormationResponse(updatedFormation);
    }

    private void updateParticipants(Formation formation, List<Long> participantsIds) {
        Set<User> newParticipants = userRepository.findAllByIdInAndRole(
                participantsIds, Role.PARTICIPANT);

        // Clear existing participants and add new ones
        formation.getParticipants().clear();
        newParticipants.forEach(formation::addParticipant);
    }

    private void updateFormateurs(Formation formation, List<Long> formateursIds) {
        Set<User> newFormateurs = userRepository.findAllByIdInAndRole(formateursIds, Role.FORMATEUR);

        // Clear existing formateurs and add new ones
        formation.getFormateurs().clear();
        newFormateurs.forEach(formation::addFormateur);
    }

    @Override
    public void deleteFormation(Long id) {
        Formation formation = verifyAndGetFormation(id);
        formationRepository.delete(formation);
    }

    private Formation verifyAndGetFormation(Long formationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found"));

        // Check if user is admin/super-admin
        if (isAdmin(authentication)) {
            return formation;
        }

        // Check if user is formateur for this formation
        String currentUserEmail = authentication.getName();
        boolean isFormateur = formation.getFormateurs().stream()
                .anyMatch(formateur -> formateur.getEmail().equals(currentUserEmail));

        if (!isFormateur) {
            throw new AccessDeniedException("You must be an admin or assigned formateur");
        }

        return formation;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") ||
                        auth.getAuthority().equals("SUPER_ADMIN"));
    }

    @Override
    public FormationResponse getFormationById(Long id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation non trouvée"));
        formation.updateEtatFormation();
        return mapToFormationResponse(formation);
    }

    @Override
    public List<FormationResponse> getFormationsByIds(List<Long> formationIds) {
        if (formationIds == null || formationIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Formation> formations = formationRepository.findAllById(formationIds);
        formations.forEach(Formation::updateEtatFormation);

        return formations.stream()
                .map(this::mapToFormationResponse)
                .collect(Collectors.toList());
    }
    @Override
    public Page<FormationResponse> getAllFormations(Pageable pageable) {
        try {
            Page<Formation> formationsPage = formationRepository.findAll(pageable);
            formationsPage.forEach(formation -> {
                formation.updateEtatFormation();
                // Ensure coupon is properly handled
                if (formation.getCoupon() != null && formation.getCoupon().getId() == null) {
                    formation.setCoupon(null);
                }
            });
            return formationsPage.map(this::mapToFormationResponse);
        } catch (Exception e) {
            LOGGER.error("Error fetching formations", e);
            throw new DataAccessException("Error fetching formations", e) {};
        }
    }


    @Override
    public Boolean toggleFormationStatus(Long id, Boolean active) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation non trouvée"));

        formation.setActive(!active);
        Formation savedFormation = formationRepository.save(formation);
        return savedFormation.getActive();
    }

    @Override
    public Page<FormationResponse> getUserFormations(Long userId, Pageable pageable) {

        // Récupérer les formations où l'utilisateur est participant ou formateur
        Page<Formation> formations = formationRepository.findUserFormations(userId, pageable);

        LOGGER.info("this the formation related to {}",formations);

        // Mettre à jour l'état de chaque formation
        formations.forEach(Formation::updateEtatFormation);
        return formations.map(this::mapToFormationResponse);
    }

    public Formation mapToEntity(CreateFormationRequest request) {
        return Formation.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .type(request.getType())
                .categorie(request.getCategorie())
                .prix(BigDecimal.valueOf(request.getPrix()))
                .niveau(request.getNiveau())
                .images(request.getImageUrls())
                .build();
    }

    public FormationResponse mapToFormationResponse(Formation formation) {
        return FormationResponse.builder()
                .id(formation.getId())
                .active(formation.getActive())
                .titre(formation.getTitre())
                .type(formation.getType() != null ? formation.getType().name() : null)
                .description(formation.getDescription())
                .dateDebut(formation.getDateDebut())
                .dateFin(formation.getDateFin())
                .categorie(formation.getCategorie())
                .prix(formation.getPrix() != null ? formation.getPrix().doubleValue() : 0.0)
                .etatFormation(formation.getEtatFormation())
                .niveau(formation.getNiveau())
                .images(formation.getImages() != null ? formation.getImages() : Collections.emptyList())
                .participantIds(formation.getParticipants().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()))
                .formateurIds(formation.getFormateurs().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()))
                .active(formation.getActive())
                .coupon(formation.getCoupon() != null ? mapToCouponDTO(formation.getCoupon()) : null)
                .build();
    }

    public CouponResponse mapToCouponDTO(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discount(coupon.getDiscount())
                .maxUsage(coupon.getMaxUsage())
                .usageCount(coupon.getUsageCount())
                .build();
    }
}