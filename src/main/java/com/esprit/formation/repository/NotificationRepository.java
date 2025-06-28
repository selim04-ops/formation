package com.esprit.formation.repository;

import com.esprit.formation.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
   // List<Notification> findByRecipientRolesContainingOrderByTimestampDesc(String role);
   // List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(String recipientId);

    @Query("SELECT DISTINCT n FROM Notification n WHERE n.recipientRoles LIKE %:role%")
    List<Notification> findByRole(@Param("role") String role);
}