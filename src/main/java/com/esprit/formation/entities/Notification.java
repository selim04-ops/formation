package com.esprit.formation.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private String id;

    private String action;          // e.g., "USER_CREATED"
    private String message;     // Detailed description
    private String status;          // "SUCCESS" or "FAILED"
    private Instant timestamp;      // When it happened

    private String method;          // Controller method name
    private String endpoint;        // API endpoint
    private String username;        // Who triggered the action
    private String recipientRoles;  // "ADMIN,SUPER_ADMIN"
    private String affectedUserEmail;
    private String affectedUserName;
    private Role affectedUserRole;
    private Boolean affectedUserActive;
    @ElementCollection
    @CollectionTable(name = "notification_user_ids", joinColumns = @JoinColumn(name = "notification_id"))
    @Column(name = "user_id")
    private List<Long> userIds = new ArrayList<>();
}
