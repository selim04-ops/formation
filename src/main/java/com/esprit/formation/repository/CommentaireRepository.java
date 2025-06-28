package com.esprit.formation.repository;

import com.esprit.formation.entities.Commentaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {


    @Query("SELECT c FROM Commentaire c WHERE c.statut.id = :statutId AND c.activeCommentaire = true")
    Page<Commentaire> findActiveByStatutId(@Param("statutId") Long statutId, Pageable pageable);


}