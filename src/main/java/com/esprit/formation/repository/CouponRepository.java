package com.esprit.formation.repository;

import com.esprit.formation.entities.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);

    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.applicableFormations")
    @EntityGraph(attributePaths = {"applicableFormations"})
    @Transactional(readOnly = true)
    Page<Coupon> findAllWithFormations(Pageable pageable);

    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.applicableFormations f WHERE c.id = :id")
    @Transactional(readOnly = true)
    @EntityGraph(attributePaths = {"applicableFormations"})
    Optional<Coupon> findByIdWithFormations(@Param("id") Long id);
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Coupon c JOIN c.applicableFormations f WHERE c.id = :couponId AND f.id = :formationId")
    boolean existsCouponWithFormation(@Param("couponId") Long couponId, @Param("formationId") Long formationId);



}