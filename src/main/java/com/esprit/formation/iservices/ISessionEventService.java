package com.esprit.formation.iservices;


import com.esprit.formation.dto.SessionEventDTO;
import com.esprit.formation.entities.FeedbackSession;
import com.esprit.formation.entities.SessionEvent;

import java.util.List;
import java.util.Set;

public interface ISessionEventService {
    SessionEvent createSessionEventWithAssociations(SessionEvent sessionEvent, Set<Long> formateurIds, Set<Long> formationIds);
    SessionEvent updateSessionEvent(Long id, SessionEvent sessionEvent);
    void deleteSessionEvent(Long id);
    SessionEventDTO getSessionEventById(Long id);
    List<SessionEventDTO> getAllSessionEvents(String type);

    SessionEvent addFormationsToSession(Long sessionId, List<Long> formationIds);
    SessionEvent addFormateursToSession(Long sessionId, List<Long> formateurIds);
    SessionEvent addParticipantsToSession(Long sessionId, List<Long> participantIds);

    FeedbackSession addFeedbackToSession(Long sessionId, FeedbackSession feedback);
    List<FeedbackSession> getSessionFeedbacks(Long sessionId);
    SessionEvent toEntity(SessionEventDTO dto);
    SessionEvent getSessionEventEntityById(Long id);

    }
