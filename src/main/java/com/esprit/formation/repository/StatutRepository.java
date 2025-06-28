package com.esprit.formation.repository;

import com.esprit.formation.entities.Statut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface StatutRepository extends JpaRepository<Statut, Long> {
    Page<Statut> findByActiveStatusTrue(Pageable pageable);

}