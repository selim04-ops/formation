package com.esprit.formation.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;


@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String id;
    private String actionType; // "USER_CREATED", "USER_DELETED", etc.
    private String message;    // Pre-formatted user-friendly message
    private String status;
    private Instant timestamp;

    // Additional structured data
    private String actorUsername;
    private NotificationData data;
    private String affectedUserEmail;
    private String affectedUserName;
    private String affectedUserRole;
    private Boolean newActiveStatus;// When it happened
    private List<Long> userIds;
    private Boolean isUnread;

}


