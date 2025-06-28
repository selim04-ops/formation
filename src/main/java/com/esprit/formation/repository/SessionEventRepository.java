package com.esprit.formation.repository;

import com.esprit.formation.entities.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {

    @Query("SELECT DISTINCT se FROM SessionEvent se " +
            "LEFT JOIN FETCH se.formations " +
            "LEFT JOIN FETCH se.formateurs " +
            "LEFT JOIN FETCH se.participants " +
            "WHERE se.type = :type")
    List<SessionEvent> findAllWithAssociationsByType(@Param("type") String type);



}