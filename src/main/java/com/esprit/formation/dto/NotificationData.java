package com.esprit.formation.dto;

import com.esprit.formation.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationData {
    private String userEmail;
    private String userName;
    private Role userRole;
    private Boolean isActive;
}
