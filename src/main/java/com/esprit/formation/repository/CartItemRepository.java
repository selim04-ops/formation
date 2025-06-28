package com.esprit.formation.repository;

import com.esprit.formation.dto.CartItemResponse;
import com.esprit.formation.entities.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByUserIdAndSessionEventIdIsNull(Long userId);
    void deleteByUserId (Long userId);
    List<CartItem> findAllByUserIdAndSessionEventIdIsNotNull(Long userId);
    void deleteByUserIdAndSessionEventIdIsNotNull(Long userId);

    @Query("SELECT new com.esprit.formation.dto.CartItemResponse(" +
            "ci.id, ci.formationId, f.titre, ci.sessionEventId, se.titre, " +
            "ci.originalPrice, ci.discountedPrice) " +
            "FROM CartItem ci " +
            "LEFT JOIN Formation f ON ci.formationId = f.id " +
            "LEFT JOIN SessionEvent se ON ci.sessionEventId = se.id " +
            "WHERE ci.userId = :userId")
    Page<CartItemResponse> getUserCartWithDetails(Long userId, Pageable pageable);

    @Query("SELECT new com.esprit.formation.dto.CartItemResponse(" +
            "ci.id, ci.formationId, f.titre, ci.sessionEventId, se.titre, " +
            "ci.originalPrice, ci.discountedPrice) " +
            "FROM CartItem ci " +
            "LEFT JOIN Formation f ON ci.formationId = f.id " +
            "LEFT JOIN SessionEvent se ON ci.sessionEventId = se.id " +
            "WHERE ci.userId = :userId " +
            "AND (:forSessionEvent = TRUE AND ci.sessionEventId IS NOT NULL " +
            "OR :forSessionEvent = FALSE AND ci.sessionEventId IS NULL)")
    Page<CartItemResponse> findUserCartWithDetails(
            @Param("userId") Long userId,
            @Param("forSessionEvent") boolean forSessionEvent,
            Pageable pageable
    );

}