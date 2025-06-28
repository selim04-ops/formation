package com.esprit.formation.repository;

import com.esprit.formation.entities.FeedbackSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackSessionRepository extends JpaRepository<FeedbackSession, Long> {

    List<FeedbackSession> findBySessionEventId(Long sessionId);
}