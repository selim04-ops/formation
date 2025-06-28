package com.esprit.formation.controller;

import com.esprit.formation.dto.NotificationDTO;
import com.esprit.formation.entities.User;
import com.esprit.formation.services.NotificationService;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "Get notifications for current user",
            description = "Returns notifications filtered by user's roles and marks unread ones.")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getCurrentUserNotifications() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            List<NotificationDTO> notifications = notificationService.getNotificationsForRoles(roles, currentUser.getId());
            return ResponseWrapper.success(notifications);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching notifications");
        }
    }

    @Operation(summary = "Mark notifications as seen",
            description = "Adds current user's ID to the notification's seen list.")
    @PostMapping("/mark-as-seen")
    public ResponseEntity<?> markNotificationsAsSeen(@RequestBody List<String> notificationIds) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = ((User) auth.getPrincipal()).getId();

            notificationService.addUserIdToNotifications(notificationIds, userId);
            return ResponseWrapper.success("Notifications marked as seen successfully");
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error marking notifications as seen");
        }
    }
}