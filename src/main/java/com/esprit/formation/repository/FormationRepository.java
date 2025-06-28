package com.esprit.formation.repository;

import com.esprit.formation.entities.Coupon;
import com.esprit.formation.entities.Formation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {
 Page<Formation> findAll(Pageable pageable);
    // Récupère uniquement les formations non supprimé:
    List<Formation> findByActiveFalse();
    Page<Formation> findByDateDebutBeforeAndDateFinAfterAndActiveFalse(
            LocalDate date1,
            LocalDate date2,
            Pageable pageable);
    Formation findFormationByCoupon(Coupon coupon);
   @Query("SELECT f FROM Formation f " +
           "LEFT JOIN f.participants p " +
           "LEFT JOIN f.formateurs fo " +
           "WHERE p.id = :userId OR fo.id = :userId")
   Page<Formation> findUserFormations(@Param("userId") Long userId, Pageable pageable);
    //Page<Formation> findByParticipantsIdOrFormateursId(Long userId, Pageable pageable);

    /*// Formations à venir (dateDebut après date actuelle)
    Page<Formation> findByDateDebutAfterAndSuprimeFormationFalse(
            LocalDate date,
            Pageable pageable);

    // Formations terminées (dateFin avant date actuelle)
    Page<Formation> findByDateFinBeforeAndSuprimeFormationFalse(
            LocalDate date,
            Pageable pageable);

    // Formations par formateur ID
    Page<Formation> findByUserIdAndActiveIsTrue(
            Long userId,
            Pageable pageable);*/
}