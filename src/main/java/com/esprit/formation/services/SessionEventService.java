package com.esprit.formation.services;

import com.esprit.formation.dto.SessionEventDTO;
import com.esprit.formation.entities.*;
import com.esprit.formation.iservices.ISessionEventService;
import com.esprit.formation.repository.FeedbackSessionRepository;
import com.esprit.formation.repository.FormationRepository;
import com.esprit.formation.repository.SessionEventRepository;
import com.esprit.formation.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionEventService implements ISessionEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionEvent.class);

    private final SessionEventRepository sessionEventRepository;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final FeedbackSessionRepository feedbackSessionRepository;

    @Transactional
    public SessionEvent createSessionEventWithAssociations(
            SessionEvent sessionEvent,
            Set<Long> formateurIds,
            Set<Long> formationIds) {

        LOGGER.info("Updating session {} with formateurs: {} and formations: {}",
                sessionEvent.getId(), formateurIds, formationIds);

        // Save the base session event first
        SessionEvent savedEvent = sessionEventRepository.save(sessionEvent);

        // Add formateurs if provided
        if (formateurIds != null && !formateurIds.isEmpty()) {
            List<User> formateurs = userRepository.findAllById(formateurIds);
            savedEvent.setFormateurs(new HashSet<>(formateurs));
        }

        // Add formations if provided
        if (formationIds != null && !formationIds.isEmpty()) {
            List<Formation> formations = formationRepository.findAllById(formationIds);
            savedEvent.setFormations(new HashSet<>(formations));
        }

        return sessionEventRepository.save(savedEvent);
    }

    @Transactional
    public SessionEvent updateSessionEvent(Long id, SessionEvent sessionEvent) {
        SessionEvent existing = getSessionEventEntityById(id);

        // Update basic fields
        existing.setTitre(sessionEvent.getTitre());
        existing.setDescription(sessionEvent.getDescription());
        existing.setType(sessionEvent.getType());
        existing.setDateDebut(sessionEvent.getDateDebut());
        existing.setDateFin(sessionEvent.getDateFin());
        existing.setLieu(sessionEvent.getLieu());
        existing.setStatut(sessionEvent.getStatut());
        existing.setCapaciteMax(sessionEvent.getCapaciteMax());
        existing.setImages(sessionEvent.getImages());

        // Only update associations if they are explicitly provided in the request
        if (sessionEvent.getFormateurs() != null && !sessionEvent.getFormateurs().isEmpty()) {
            existing.getFormateurs().clear();
            existing.getFormateurs().addAll(sessionEvent.getFormateurs());
        }

        if (sessionEvent.getFormations() != null && !sessionEvent.getFormations().isEmpty()) {
            existing.getFormations().clear();
            existing.getFormations().addAll(sessionEvent.getFormations());
        }

        if (sessionEvent.getParticipants() != null && !sessionEvent.getParticipants().isEmpty()) {
            existing.getParticipants().clear();
            existing.getParticipants().addAll(sessionEvent.getParticipants());
        }

        return sessionEventRepository.save(existing);
    }

    @Override
    public void deleteSessionEvent(Long id) {
        sessionEventRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionEventDTO getSessionEventById(Long id) {
        SessionEvent sessionEvent = getSessionEventEntityById(id);
        return SessionEventDTO.fromEntity(sessionEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionEventDTO> getAllSessionEvents(String type) {
        return sessionEventRepository.findAllWithAssociationsByType(type).stream()
                .map(SessionEventDTO::fromEntity)
                .collect(Collectors.toList());
    }




    @Override
    @Transactional
    public SessionEvent addFormationsToSession(Long sessionId, List<Long> formationIds) {
        SessionEvent session = getSessionEventEntityById(sessionId);
        List<Formation> formations = formationRepository.findAllById(formationIds);
        if (formations.size() != formationIds.size()) {
            throw new EntityNotFoundException("Some formations not found");
        }
        session.getFormations().addAll(formations);
        return sessionEventRepository.save(session);
    }

    @Override
    @Transactional
    public SessionEvent addFormateursToSession(Long sessionId, List<Long> formateurIds) {
        SessionEvent session = getSessionEventEntityById(sessionId);
        Set<User> formateurs = userRepository.findAllByIdInAndRole(formateurIds, Role.FORMATEUR);
        if (formateurs.size() != formateurIds.size()) {
            throw new EntityNotFoundException("Some formateurs not found or don't have FORMATEUR role");
        }
        session.getFormateurs().addAll(formateurs);
        return sessionEventRepository.save(session);
    }

    @Override
    @Transactional
    public SessionEvent addParticipantsToSession(Long sessionId, List<Long> participantIds) {
        SessionEvent session = getSessionEventEntityById(sessionId);
        Set<User> participants = userRepository.findAllByIdInAndRole(participantIds, Role.PARTICIPANT);
        if (participants.size() != participantIds.size()) {
            throw new EntityNotFoundException("Some participants not found or don't have PARTICIPANT role");
        }
        session.getParticipants().addAll(participants);
        return sessionEventRepository.save(session);
    }

    @Override
    public FeedbackSession addFeedbackToSession(Long sessionId, FeedbackSession feedback) {
        SessionEvent session = getSessionEventEntityById(sessionId);
        feedback.setSessionEvent(session);
        return feedbackSessionRepository.save(feedback);
    }

    @Override
    public List<FeedbackSession> getSessionFeedbacks(Long sessionId) {
        return feedbackSessionRepository.findBySessionEventId(sessionId);
    }


    @Override

    public SessionEvent getSessionEventEntityById(Long id) {
        return sessionEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SessionEvent not found with id: " + id));
    }

    public SessionEvent toEntity(SessionEventDTO dto) {
        SessionEvent sessionEvent = new SessionEvent();
        sessionEvent.setId(dto.getId());
        sessionEvent.setTitre(dto.getTitre());
        sessionEvent.setDescription(dto.getDescription());
        sessionEvent.setType(dto.getType());
        sessionEvent.setDateDebut(dto.getDateDebut());
        sessionEvent.setDateFin(dto.getDateFin());
        sessionEvent.setLieu(dto.getLieu());
        sessionEvent.setStatut(dto.getStatut());
        sessionEvent.setCapaciteMax(dto.getCapaciteMax());
        sessionEvent.setImages(dto.getImages() != null ? dto.getImages() : Collections.emptyList());

        if (dto.getFormateurIds() != null && !dto.getFormateurIds().isEmpty()) {
            Set<User> formateurs = userRepository.findAllByIdInAndRole(
                    new ArrayList<>(dto.getFormateurIds()), Role.FORMATEUR
            );
            sessionEvent.setFormateurs(formateurs);
        } else {
            // Preserve existing formateurs if not provided
            if (dto.getId() != null) {
                SessionEvent existing = getSessionEventEntityById(dto.getId());
                sessionEvent.setFormateurs(existing.getFormateurs());
            }
        }

        // Handle formations if IDs are provided
        if (dto.getFormationIds() != null && !dto.getFormationIds().isEmpty()) {
            Set<Formation> formations = new HashSet<>(formationRepository.findAllById(dto.getFormationIds()));
            sessionEvent.setFormations(formations);
        } else {
            // Preserve existing formations if not provided
            if (dto.getId() != null) {
                SessionEvent existing = getSessionEventEntityById(dto.getId());
                sessionEvent.setFormations(existing.getFormations());
            }
        }

        return sessionEvent;
    }
}