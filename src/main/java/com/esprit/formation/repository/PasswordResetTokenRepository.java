package com.esprit.formation.repository;

import com.esprit.formation.entities.PasswordResetToken;
import com.esprit.formation.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser (User user);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId AND t.isAvailable = true")
    Optional<PasswordResetToken> findActiveTokenByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.isAvailable = false " +
            "WHERE (t.expiryDate < :currentDate OR t.isAvailable = true) " +
            "AND t.expiryDate < :currentDate")
    int deactivateExpiredTokens(@Param("currentDate") Date currentDate);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId AND t.isAvailable = false")
    List<PasswordResetToken> findByUserIdAndIsAvailableFalse(@Param("userId") Long userId);

}